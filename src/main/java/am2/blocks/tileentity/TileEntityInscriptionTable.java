package am2.blocks.tileentity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import am2.ArsMagica2;
import am2.affinity.Affinity;
import am2.api.SpellRegistry;
import am2.api.SpellRegistry.SpellData;
import am2.container.ContainerInscriptionTable;
import am2.defs.ItemDefs;
import am2.event.SpellRecipeItemsEvent;
import am2.items.ItemSpellComponent;
import am2.lore.Story;
import am2.packet.AMDataReader;
import am2.packet.AMDataWriter;
import am2.packet.AMNetHandler;
import am2.packet.AMPacketIDs;
import am2.particles.AMParticle;
import am2.particles.ParticleFloatUpward;
import am2.particles.ParticleHoldPosition;
import am2.power.PowerTypes;
import am2.skill.Skill;
import am2.spell.IComponent;
import am2.spell.IModifier;
import am2.spell.IShape;
import am2.spell.ISpellPart;
import am2.spell.SpellModifiers;
import am2.spell.SpellValidator;
import am2.utils.KeyValuePair;
import am2.utils.RecipeUtils;
import am2.utils.SpellUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityInscriptionTable extends TileEntity implements IInventory, ITickable{

	private ItemStack inscriptionTableItemStacks[];
	private final ArrayList<ISpellPart> currentRecipe;
	private final ArrayList<ArrayList<ISpellPart>> shapeGroups;
	private int numStageGroups = 2;
	public static final int MAX_STAGE_GROUPS = 5;
	public static int bookIndex = 0;
	public static int paperIndex = 1;
	public static int featherIndex = 2;
	public static int inkIndex = 3;
	private EntityPlayer currentPlayerUsing;
	private int ticksToNextParticle = 20;
	private final HashMap<SpellModifiers, Integer> modifierCount;
	private String currentSpellName;
	private boolean currentSpellIsReadOnly;

	private static final byte FULL_UPDATE = 0x1;
	private static final byte MAKE_SPELL = 0x2;
	private static final byte RESET_NAME = 0x4;

	public TileEntityInscriptionTable(){
		inscriptionTableItemStacks = new ItemStack[getSizeInventory()];
		currentPlayerUsing = null;
		currentSpellName = "";
		currentRecipe = new ArrayList<ISpellPart>();
		shapeGroups = new ArrayList<ArrayList<ISpellPart>>();

		for (int i = 0; i < MAX_STAGE_GROUPS; ++i){
			shapeGroups.add(new ArrayList<ISpellPart>());
		}

		modifierCount = new HashMap<SpellModifiers, Integer>();
		resetModifierCount();
	}

	public ArrayList<ISpellPart> getCurrentRecipe(){
		return this.currentRecipe;
	}

	@Override
	public int getSizeInventory(){
		return 4;
	}

	@Override
	public ItemStack getStackInSlot(int i){
		return inscriptionTableItemStacks[i];
	}

	@Override
	public ItemStack decrStackSize(int i, int j){
		if (inscriptionTableItemStacks[i] != null){
			if (inscriptionTableItemStacks[i].stackSize <= j){
				ItemStack itemstack = inscriptionTableItemStacks[i];
				inscriptionTableItemStacks[i] = null;
				return itemstack;
			}
			ItemStack itemstack1 = inscriptionTableItemStacks[i].splitStack(j);
			if (inscriptionTableItemStacks[i].stackSize == 0){
				inscriptionTableItemStacks[i] = null;
			}
			return itemstack1;
		}else{
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack){
		inscriptionTableItemStacks[i] = itemstack;
		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()){
			itemstack.stackSize = getInventoryStackLimit();
		}
	}

	@Override
	public String getName(){
		return "Inscription Table";
	}

	@Override
	public int getInventoryStackLimit(){
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer){
		if (worldObj.getTileEntity(pos) != this){
			return false;
		}
		return entityplayer.getDistanceSqToCenter(pos) <= 64D;
	}

	public boolean isInUse(EntityPlayer player){
		return currentPlayerUsing != null && currentPlayerUsing.getEntityId() != player.getEntityId();
	}

	public void setInUse(EntityPlayer player){
		this.currentPlayerUsing = player;
		if (!this.worldObj.isRemote){
			worldObj.markAndNotifyBlock(pos, worldObj.getChunkFromBlockCoords(pos), worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
		}
	}

	public EntityPlayer getCurrentPlayerUsing(){
		return this.currentPlayerUsing;
	}

	private boolean isRenderingLeft(){
		return (getBlockMetadata() & 0x8) == 0x8;
	}

	@Override
	public void update(){
		if (worldObj.isRemote && getUpgradeState() >= 3)
			candleUpdate();

		if (this.numStageGroups > MAX_STAGE_GROUPS)
			this.numStageGroups = MAX_STAGE_GROUPS;
	}

	public int getUpgradeState(){
		return this.numStageGroups - 2;
	}

	private void candleUpdate(){
		ticksToNextParticle--;

		if (isRenderingLeft()){
			if (ticksToNextParticle == 0 || ticksToNextParticle == 15){

				int meta = getBlockMetadata() & ~0x8;

				double particleX = 0;
				double particleZ = 0;

				switch (meta){
				case 1:
					particleX = this.pos.getX() + 0.15;
					particleZ = this.getPos().getZ() + 0.22;
					break;
				case 2:
					particleX = this.getPos().getX() + 0.22;
					particleZ = this.getPos().getZ() + 0.85;
					break;
				case 3:
					particleX = this.getPos().getX() + 0.83;
					particleZ = this.getPos().getZ() + 0.78;
					break;
				case 4:
					particleX = this.getPos().getX() + 0.79;
					particleZ = this.getPos().getZ() + 0.15;
					break;
				}

				ticksToNextParticle = 30;
				AMParticle effect = (AMParticle)ArsMagica2.proxy.particleManager.spawn(worldObj, "fire", particleX, getPos().getY() + 1.32, particleZ);
				if (effect != null){
					effect.setParticleScale(0.025f, 0.1f, 0.025f);
					effect.AddParticleController(new ParticleHoldPosition(effect, 29, 1, false));
					effect.setIgnoreMaxAge(false);
					effect.setMaxAge(400);
				}

				if (worldObj.rand.nextInt(100) > 80){
					AMParticle smoke = (AMParticle)ArsMagica2.proxy.particleManager.spawn(worldObj, "smoke", particleX, getPos().getY() + 1.4, particleZ);
					if (smoke != null){
						smoke.setParticleScale(0.025f);
						smoke.AddParticleController(new ParticleFloatUpward(smoke, 0.01f, 0.01f, 1, false));
						smoke.setIgnoreMaxAge(false);
						smoke.setMaxAge(20 + worldObj.rand.nextInt(10));
					}
				}
			}
			if (ticksToNextParticle == 10 || ticksToNextParticle == 25){

				int meta = getBlockMetadata() & ~0x8;

				double particleX = 0;
				double particleZ = 0;

				switch (meta){
				case 1:
					particleX = this.getPos().getX() + 0.59;
					particleZ = this.getPos().getZ() - 0.72;
					break;
				case 2:
					particleX = this.getPos().getX() - 0.72;
					particleZ = this.getPos().getZ() + 0.41;
					break;
				case 3:
					particleX = this.getPos().getX() + 0.41;
					particleZ = this.getPos().getZ() + 1.72;
					break;
				case 4:
					particleX = this.getPos().getX() + 1.72;
					particleZ = this.getPos().getZ() + 0.60;
					break;
				}

				AMParticle effect = (AMParticle)ArsMagica2.proxy.particleManager.spawn(worldObj, "fire", particleX, getPos().getY() + 1.26, particleZ);
				if (effect != null){
					effect.setParticleScale(0.025f, 0.1f, 0.025f);
					effect.AddParticleController(new ParticleHoldPosition(effect, 29, 1, false));
					effect.setIgnoreMaxAge(false);
					effect.setMaxAge(400);
				}

				if (worldObj.rand.nextInt(100) > 80){
					AMParticle smoke = (AMParticle)ArsMagica2.proxy.particleManager.spawn(worldObj, "smoke", particleX, getPos().getY() + 1.4, particleZ);
					if (smoke != null){
						smoke.setParticleScale(0.025f);
						smoke.AddParticleController(new ParticleFloatUpward(smoke, 0.01f, 0.01f, 1, false));
						smoke.setIgnoreMaxAge(false);
						smoke.setMaxAge(20 + worldObj.rand.nextInt(10));
					}
				}
			}

		}
	}

	@Override
	public void openInventory(EntityPlayer player){
	}

	@Override
	public void closeInventory(EntityPlayer player){
	}

	@Override
	public ItemStack removeStackFromSlot(int i){
		if (inscriptionTableItemStacks[i] != null){
			ItemStack itemstack = inscriptionTableItemStacks[i];
			inscriptionTableItemStacks[i] = null;
			return itemstack;
		}else{
			return null;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound){
		super.readFromNBT(par1NBTTagCompound);
		parseTagCompound(par1NBTTagCompound);
		clearCurrentRecipe();
	}

	private void parseTagCompound(NBTTagCompound par1NBTTagCompound){
		NBTTagList nbttaglist = par1NBTTagCompound.getTagList("InscriptionTableInventory", Constants.NBT.TAG_COMPOUND);
		inscriptionTableItemStacks = new ItemStack[getSizeInventory()];
		for (int i = 0; i < nbttaglist.tagCount(); i++){
			String tag = String.format("ArrayIndex", i);
			NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.getCompoundTagAt(i);
			byte byte0 = nbttagcompound1.getByte(tag);
			if (byte0 >= 0 && byte0 < inscriptionTableItemStacks.length){
				inscriptionTableItemStacks[byte0] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
			}
		}

		this.numStageGroups = Math.max(par1NBTTagCompound.getInteger("numShapeGroupSlots"), 2);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound par1NBTTagCompound){
		super.writeToNBT(par1NBTTagCompound);
		NBTTagList nbttaglist = new NBTTagList();
		for (int i = 0; i < inscriptionTableItemStacks.length; i++){
			if (inscriptionTableItemStacks[i] != null){
				String tag = String.format("ArrayIndex", i);
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte(tag, (byte)i);
				inscriptionTableItemStacks[i].writeToNBT(nbttagcompound1);
				nbttaglist.appendTag(nbttagcompound1);
			}
		}

		par1NBTTagCompound.setTag("InscriptionTableInventory", nbttaglist);
		par1NBTTagCompound.setInteger("numShapeGroupSlots", this.numStageGroups);
		return par1NBTTagCompound;
	}

	@Override
	public boolean hasCustomName(){
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack){
		return false;
	}

	public void HandleUpdatePacket(byte[] data){
		if (this.worldObj == null)
			return;
		AMDataReader rdr = new AMDataReader(data);
		switch (rdr.ID){
		case FULL_UPDATE:
			if (!rdr.getBoolean()){
				Entity e = this.worldObj.getEntityByID(rdr.getInt());
				if (e instanceof EntityPlayer){
					EntityPlayer player = (EntityPlayer)e;
					this.setInUse(player);
				}else{
					this.setInUse(null);
				}
			}else{
				this.setInUse(null);
			}

			currentRecipe.clear();
			int partLength = rdr.getInt();
			for (int i = 0; i < partLength; ++i){
				Skill part = ItemSpellComponent.getPartFor(rdr.getInt());
				SpellData<? extends ISpellPart> spellData = SpellRegistry.getCombinedMap().get(part.getID());
				if (spellData != null)
					this.currentRecipe.add(spellData.part);
			}

			this.shapeGroups.clear();
			int numGroups = rdr.getInt();
			for (int i = 0; i < numGroups; ++i){
				ArrayList<ISpellPart> group = new ArrayList<ISpellPart>();
				int[] partData = rdr.getIntArray();
				for (int n : partData){
					Skill part = ItemSpellComponent.getPartFor(n);
					SpellData<? extends ISpellPart> spellData = SpellRegistry.getCombinedMap().get(part.getID());
					if (spellData != null)
						group.add(spellData.part);
				}
				this.shapeGroups.add(group);
			}

			countModifiers();
			this.currentSpellName = rdr.getString();
			this.currentSpellIsReadOnly = rdr.getBoolean();
			break;
		case MAKE_SPELL:
			int entityID = rdr.getInt();
			EntityPlayer player = (EntityPlayer)worldObj.getEntityByID(entityID);
			if (player != null){
				createSpellForPlayer(player);
			}
			break;
		case RESET_NAME:
			entityID = rdr.getInt();
			player = (EntityPlayer)worldObj.getEntityByID(entityID);
			if (player != null){
				((ContainerInscriptionTable)player.openContainer).resetSpellNameAndIcon();
			}
			break;
		}
	}

	private byte[] GetUpdatePacketForServer(){
		AMDataWriter writer = new AMDataWriter();
		writer.add(FULL_UPDATE);
		writer.add(this.currentPlayerUsing == null);
		if (this.currentPlayerUsing != null) writer.add(this.currentPlayerUsing.getEntityId());

		writer.add(this.currentRecipe.size());
		for (int i = 0; i < this.currentRecipe.size(); ++i){
			ISpellPart part = this.currentRecipe.get(i);
			Skill skill = SpellRegistry.getSkillFromPart(part);
			writer.add(ItemSpellComponent.getIdFor(skill));
		}

		writer.add(this.shapeGroups.size());
		for (ArrayList<ISpellPart> shapeGroup : this.shapeGroups){
			int[] groupData = new int[shapeGroup.size()];
			for (int i = 0; i < shapeGroup.size(); ++i){
				groupData[i] = ItemSpellComponent.getIdFor(SpellRegistry.getSkillFromPart(shapeGroup.get(i)));
			}
			writer.add(groupData);
		}

		writer.add(currentSpellName);
		writer.add(currentSpellIsReadOnly);

		return writer.generate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket(){
		NBTTagCompound compound = new NBTTagCompound();
		this.writeToNBT(compound);
		SPacketUpdateTileEntity packet = new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), compound);
		return packet;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
		this.parseTagCompound(pkt.getNbtCompound());
	}

	private void sendDataToServer(){
		AMDataWriter writer = new AMDataWriter();
		writer.add(getPos().getX());
		writer.add(getPos().getY());
		writer.add(getPos().getZ());
		writer.add(GetUpdatePacketForServer());

		AMNetHandler.INSTANCE.sendPacketToServer(AMPacketIDs.INSCRIPTION_TABLE_UPDATE, writer.generate());
	}

	public void addSpellPartToStageGroup(int groupIndex, ISpellPart part){
		ArrayList<ISpellPart> group = this.shapeGroups.get(groupIndex);
		if (!currentSpellIsReadOnly && group.size() < 4 && !(part instanceof IComponent)){
			group.add(part);
			if (this.worldObj.isRemote)
				this.sendDataToServer();
			countModifiers();
		}
	}

	public void removeSpellPartFromStageGroup(int index, int groupIndex){
		ArrayList<ISpellPart> group = this.shapeGroups.get(groupIndex);
		if (!this.currentSpellIsReadOnly){
			group.remove(index);
			if (this.worldObj.isRemote)
				this.sendDataToServer();
			countModifiers();
		}
	}

	public void removeMultipleSpellPartsFromStageGroup(int startIndex, int length, int groupIndex){
		ArrayList<ISpellPart> group = this.shapeGroups.get(groupIndex);
		if (!currentSpellIsReadOnly){
			for (int i = 0; i <= length; ++i)
				group.remove(startIndex);
			countModifiers();
			if (this.worldObj.isRemote)
				this.sendDataToServer();
		}
	}

	public void addSpellPart(ISpellPart part){
		if (!currentSpellIsReadOnly && this.currentRecipe.size() < 16){
			this.currentRecipe.add(part);
			if (this.worldObj.isRemote)
				this.sendDataToServer();
			countModifiers();
		}
	}

	public void removeSpellPart(int index){
		if (!this.currentSpellIsReadOnly){
			this.currentRecipe.remove(index);
			if (this.worldObj.isRemote)
				this.sendDataToServer();
			countModifiers();
		}
	}

	public void removeMultipleSpellParts(int startIndex, int length){
		if (!currentSpellIsReadOnly){
			for (int i = 0; i <= length; ++i)
				this.getCurrentRecipe().remove(startIndex);
			countModifiers();
			if (this.worldObj.isRemote)
				this.sendDataToServer();
		}
	}

	public int getNumStageGroups(){
		return this.numStageGroups;
	}

	private void countModifiers(){

		resetModifierCount();

		for (ArrayList<ISpellPart> shapeGroup : this.shapeGroups){
			countModifiersInList(shapeGroup);
		}

		ArrayList<ArrayList<ISpellPart>> stages = SpellValidator.splitToStages(currentRecipe);
		if (stages.size() == 0) return;
		ArrayList<ISpellPart> currentStage = stages.get(stages.size() - 1);
		countModifiersInList(currentStage);
	}

	private void countModifiersInList(ArrayList<ISpellPart> currentStage){
		for (ISpellPart part : currentStage){
			if (part instanceof IModifier){
				EnumSet<SpellModifiers> modifiers = ((IModifier)part).getAspectsModified();
				for (SpellModifiers modifier : modifiers){
					int count = modifierCount.get(modifier) + 1;
					modifierCount.put(modifier, count);
				}
			}
		}
	}

	private void resetModifierCount(){
		modifierCount.clear();
		for (SpellModifiers modifier : SpellModifiers.values()){
			modifierCount.put(modifier, 0);
		}
	}

	public int getModifierCount(SpellModifiers modifier){
		return modifierCount.get(modifier);
	}

	public void createSpellForPlayer(EntityPlayer player){
		if (worldObj.isRemote){
			AMDataWriter writer = new AMDataWriter();
			writer.add(getPos().getX());
			writer.add(getPos().getY());
			writer.add(getPos().getZ());
			writer.add(MAKE_SPELL);
			writer.add(player.getEntityId());
			AMNetHandler.INSTANCE.sendPacketToServer(AMPacketIDs.INSCRIPTION_TABLE_UPDATE, writer.generate());
		}else{

			ArrayList<KeyValuePair<ArrayList<ISpellPart>, NBTTagCompound>> shapeGroupSetup = new ArrayList<>();
			KeyValuePair<ArrayList<ISpellPart>, NBTTagCompound> curRecipeSetup = new KeyValuePair<>(currentRecipe, new NBTTagCompound());

			for (ArrayList<ISpellPart> arr : shapeGroups){
				shapeGroupSetup.add(new KeyValuePair<ArrayList<ISpellPart>, NBTTagCompound>(arr, new NBTTagCompound()));
				for (ISpellPart part : arr) {
					System.out.println(part.getClass().getSimpleName());
				}
			}
			for (ISpellPart part : curRecipeSetup.key) {
				System.out.println(part.getClass().getSimpleName());
			}
			ItemStack stack = SpellUtils.createSpellStack(shapeGroupSetup, curRecipeSetup);

			stack.getTagCompound().setString("suggestedName", currentSpellName);

			player.inventory.addItemStackToInventory(stack);
		}
	}

	public ItemStack writeRecipeAndDataToBook(ItemStack bookstack, EntityPlayer player, String title){
		if (bookstack.getItem() == Items.WRITTEN_BOOK && this.currentRecipe != null){
			if (!currentRecipeIsValid().valid)
				return bookstack;

			if (!bookstack.hasTagCompound())
				bookstack.setTagCompound(new NBTTagCompound());
			else if (bookstack.getTagCompound().getBoolean("spellFinalized")) //don't overwrite a completed spell
				return bookstack;

			LinkedHashMap<String, Integer> materialsList = new LinkedHashMap<String, Integer>();

			materialsList.put(ItemDefs.blankRune.getItemStackDisplayName(new ItemStack(ItemDefs.blankRune)), 1);

			ArrayList<ItemStack> componentRecipeList = new ArrayList<ItemStack>();
			ArrayList<ISpellPart> allRecipeItems = new ArrayList<ISpellPart>();

			for (ArrayList<ISpellPart> shapeGroup : shapeGroups){
				if (shapeGroup == null || shapeGroup.size() == 0)
					continue;
				allRecipeItems.addAll(shapeGroup);
			}

			allRecipeItems.addAll(currentRecipe);
			for (ISpellPart part : allRecipeItems){
				
				if (part == null){
					ArsMagica2.LOGGER.error("Unable to write recipe to book.  Recipe part is null!");
					return bookstack;
				}
				
				Object[] recipeItems = part.getRecipe();
				SpellRecipeItemsEvent event = new SpellRecipeItemsEvent(SpellRegistry.getSkillFromPart(part).getID(), recipeItems);
				MinecraftForge.EVENT_BUS.post(event);
				recipeItems = event.recipeItems;

				if (recipeItems == null){
					ArsMagica2.LOGGER.error("Unable to write recipe to book.  Recipe items are null for part %s!", SpellRegistry.getSkillFromPart(part).getName());
					return bookstack;
				}
				for (int i = 0; i < recipeItems.length; ++i){
					Object o = recipeItems[i];
					String materialkey = "";
					int qty = 1;
					ItemStack recipeStack = null;
					if (o instanceof ItemStack){
						materialkey = ((ItemStack)o).getDisplayName();
						recipeStack = (ItemStack)o;
					}else if (o instanceof Item){
						recipeStack = new ItemStack((Item)o);
						materialkey = ((Item)o).getItemStackDisplayName(new ItemStack((Item)o));
					}else if (o instanceof Block){
						recipeStack = new ItemStack((Block)o);
						materialkey = ((Block)o).getLocalizedName();
					}else if (o instanceof String){
						if (((String)o).startsWith("E:")){
							int[] ids = RecipeUtils.ParseEssenceIDs((String)o);
							materialkey = "Essence (";
							for (int powerID : ids){
								PowerTypes type = PowerTypes.getByID(powerID);
								materialkey += type.name() + "/";
							}

							if (materialkey.equals("Essence (")){
								++i;
								continue;
							}

							o = recipeItems[++i];
							if (materialkey.startsWith("Essence (")){
								materialkey = materialkey.substring(0, materialkey.lastIndexOf("/")) + ")";
								qty = (Integer)o;
								int flag = 0;
								for (int f : ids){
									flag |= f;
								}

								recipeStack = new ItemStack(ItemDefs.etherium, qty, flag);
							}

						}else{
							List<ItemStack> ores = OreDictionary.getOres((String)o);
							recipeStack = ores.size() > 0 ? ores.get(1) : null;
							materialkey = (String)o;
						}
					}

					if (materialsList.containsKey(materialkey)){
						int old = materialsList.get(materialkey);
						old += qty;
						materialsList.put(materialkey, old);
					}else{
						materialsList.put(materialkey, qty);
					}

					if (recipeStack != null)
						componentRecipeList.add(recipeStack);
				}
			}

			materialsList.put(ItemDefs.spellParchment.getItemStackDisplayName(new ItemStack(ItemDefs.spellParchment)), 1);

			StringBuilder sb = new StringBuilder();
			int sgCount = 0;
			int[][] shapeGroupCombos = new int[shapeGroups.size()][];
			for (ArrayList<ISpellPart> shapeGroup : shapeGroups){
				sb.append("Shape Group " + ++sgCount + "\n\n");
				Iterator<ISpellPart> it = shapeGroup.iterator();
				shapeGroupCombos[sgCount - 1] = SpellPartListToStringBuilder(it, sb, " -");
				sb.append("\n");
			}

			sb.append("Combination:\n\n");
			Iterator<ISpellPart> it = currentRecipe.iterator();
			int[] outputData = SpellPartListToStringBuilder(it, sb, null);
			System.out.println(sb.toString());

			ArrayList<NBTTagString> pages = Story.splitStoryPartIntoPages(sb.toString());

			sb = new StringBuilder();
			sb.append("\n\nMaterials List:\n\n");
			for (String s : materialsList.keySet()){
				sb.append(materialsList.get(s) + " x " + s + "\n");
			}

			pages.addAll(Story.splitStoryPartIntoPages(sb.toString()));

			sb = new StringBuilder();
			sb.append("Affinity Breakdown:\n\n");
			it = currentRecipe.iterator();
			HashMap<Affinity, Integer> affinityData = new HashMap<Affinity, Integer>();
			int cpCount = 0;
			while (it.hasNext()){
				ISpellPart part = it.next();
				if (part instanceof IComponent){
					Set<Affinity> aff = ((IComponent)part).getAffinity();
					for (Affinity affinity : aff){
						int qty = 1;
						if (affinityData.containsKey(affinity)){
							qty = 1 + affinityData.get(affinity);
						}
						affinityData.put(affinity, qty);
					}
					cpCount++;
				}
			}
			ValueComparator vc = new ValueComparator(affinityData);
			TreeMap<Affinity, Integer> sorted = new TreeMap<Affinity, Integer>(vc);
			sorted.putAll(affinityData);
			for (Affinity aff : sorted.keySet()){
				float pct = (float)sorted.get(aff) / (float)cpCount * 100f;
				sb.append(String.format("%s: %.2f%%", aff.getLocalizedName(), pct));
				sb.append("\n");
			}
			pages.addAll(Story.splitStoryPartIntoPages(sb.toString()));
			Story.WritePartToNBT(bookstack.getTagCompound(), pages);
	
			bookstack = Story.finalizeStory(bookstack, title, player.getName());

			int[] recipeData = new int[componentRecipeList.size() * 3];
			int idx = 0;
			for (ItemStack stack : componentRecipeList){
				recipeData[idx++] = Item.getIdFromItem(stack.getItem());
				recipeData[idx++] = stack.stackSize;
				recipeData[idx++] = stack.getItemDamage();
			}

			bookstack.getTagCompound().setIntArray("spell_combo", recipeData);
			bookstack.getTagCompound().setIntArray("output_combo", outputData);
			bookstack.getTagCompound().setInteger("numShapeGroups", shapeGroupCombos.length);
			int index = 0;
			for (int[] sgArray : shapeGroupCombos){
				bookstack.getTagCompound().setIntArray("shapeGroupCombo_" + index++, sgArray);
			}
			bookstack.getTagCompound().setString("spell_mod_version", ArsMagica2.instance.getVersion());

			if (currentSpellName.equals(""))
				currentSpellName = "Spell Recipe";
			bookstack.setStackDisplayName(currentSpellName);

			this.currentRecipe.clear();
			for (ArrayList<ISpellPart> list : shapeGroups)
				list.clear();
			currentSpellName = "";

			bookstack.getTagCompound().setBoolean("spellFinalized", true);

			//worldObj.playSound(getPos().getX(), getPos().getY(), getPos().getZ(), "arsmagica2:misc.inscriptiontable.takebook", 1.0f, 1.0f, true);

			worldObj.markAndNotifyBlock(pos, worldObj.getChunkFromBlockCoords(pos), worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
		}
		return bookstack;
	}

	private int[] SpellPartListToStringBuilder(Iterator<ISpellPart> it, StringBuilder sb, String prefix){
		ArrayList<Integer> outputCombo = new ArrayList<Integer>();
		while (it.hasNext()){
			ISpellPart part = it.next();
			String displayName = SpellRegistry.getSkillFromPart(part).getName();

			if (prefix != null){
				sb.append(prefix + displayName + "\n");
			}else{
				if (part instanceof IShape){
					sb.append(displayName + "\n");
				}else{
					sb.append("-" + displayName + "\n");
				}
			}

			outputCombo.add(ItemSpellComponent.getIdFor(SpellRegistry.getSkillFromPart(part)));
		}

		int[] outputData = new int[outputCombo.size()];
		int idx = 0;
		for (Integer I : outputCombo){
			outputData[idx++] = I;
		}

		return outputData;
	}

	public void clearCurrentRecipe(){
		this.currentRecipe.clear();
		for (ArrayList<ISpellPart> group : shapeGroups)
			group.clear();
		currentSpellName = "";
		currentSpellIsReadOnly = false;
		if (worldObj != null)
			worldObj.markAndNotifyBlock(pos, worldObj.getChunkFromBlockCoords(pos), worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
	}

	public SpellValidator.ValidationResult currentRecipeIsValid(){
		ArrayList<ArrayList<ISpellPart>> segmented = SpellValidator.splitToStages(currentRecipe);
		return SpellValidator.instance.spellDefIsValid(shapeGroups, segmented);
	}

	public boolean modifierCanBeAdded(IModifier modifier){
		return false;
	}

	static class ValueComparator implements Comparator<Affinity>{

		Map<Affinity, Integer> base;

		ValueComparator(Map<Affinity, Integer> base){
			this.base = base;
		}

		@Override
		public int compare(Affinity a, Affinity b){
			Integer x = base.get(a);
			Integer y = base.get(b);
			if (x.equals(y)){
				return a.compareTo(b);
			}
			return x.compareTo(y);
		}
	}

	public void setSpellName(String name){
		this.currentSpellName = name;
		sendDataToServer();
	}

	public String getSpellName(){
		return this.currentSpellName != null ? this.currentSpellName : "";
	}

	public void reverseEngineerSpell(ItemStack stack){
		this.currentRecipe.clear();
		for (ArrayList<ISpellPart> group : shapeGroups){
			group.clear();
		}
		currentSpellName = "";

		this.currentSpellName = stack.getDisplayName();
		int numStages = SpellUtils.numStages(stack);

		for (int i = 0; i < numStages; ++i){
			IShape shape = SpellUtils.getShapeForStage(stack, i);
			this.currentRecipe.add(shape);
			ArrayList<IComponent> components = SpellUtils.getComponentsForStage(stack, i);
			for (IComponent component : components)
				this.currentRecipe.add(component);
			ArrayList<IModifier> modifiers = SpellUtils.getModifiersForStage(stack, i);
			for (IModifier modifier : modifiers)
				this.currentRecipe.add(modifier);
		}

		int numShapeGroups = SpellUtils.numShapeGroups(stack);
		for (int i = 0; i < numShapeGroups; ++i){
			ArrayList<ISpellPart> parts = SpellUtils.getShapeGroupParts(stack, i);
			for (ISpellPart partID : parts){
				if (partID != null && partID instanceof ISpellPart)
					this.shapeGroups.get(i).add(partID);
			}
		}

		currentSpellIsReadOnly = true;
	}

	public boolean currentSpellDefIsReadOnly(){
		return this.currentSpellIsReadOnly;
	}

	public void resetSpellNameAndIcon(ItemStack stack, EntityPlayer player){
		if (worldObj.isRemote){
			AMDataWriter writer = new AMDataWriter();
			writer.add(getPos().getX());
			writer.add(getPos().getY());
			writer.add(getPos().getZ());
			writer.add(RESET_NAME);
			writer.add(player.getEntityId());
			AMNetHandler.INSTANCE.sendPacketToServer(AMPacketIDs.INSCRIPTION_TABLE_UPDATE, writer.generate());
		}
		stack.setItemDamage(0);
		stack.clearCustomName();
	}

	public int getShapeGroupSize(int groupIndex){
		return this.shapeGroups.get(groupIndex).size();
	}

	public ISpellPart getShapeGroupPartAt(int groupIndex, int index){
		return this.shapeGroups.get(groupIndex).get(index);
	}


	public void incrementUpgradeState(){
		this.numStageGroups++;
		if (!this.worldObj.isRemote){
			List<EntityPlayerMP> players = this.worldObj.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(pos).expand(256, 256, 256));
			for (EntityPlayerMP player : players){
				player.connection.sendPacket(getUpdatePacket());
			}
		}
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentString(getName());
	}

	@Override
	public int getField(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setField(int id, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getFieldCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}
}