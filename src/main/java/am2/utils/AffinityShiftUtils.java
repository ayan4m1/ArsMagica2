package am2.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import am2.api.ArsMagicaAPI;
import am2.api.affinity.Affinity;
import am2.api.event.AffinityChangingEvent;
import am2.api.extensions.IAffinityData;
import am2.armor.ArmorHelper;
import am2.armor.infusions.GenericImbuement;
import am2.defs.ItemDefs;
import am2.defs.SkillDefs;
import am2.extensions.AffinityData;
import am2.extensions.EntityExtension;
import am2.extensions.SkillData;
import am2.spell.SpellComponent;
import am2.spell.SpellShape;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public class AffinityShiftUtils {
	
	public static void doAffinityShift(EntityLivingBase caster, SpellComponent component, SpellShape governingShape){
		if (!(caster instanceof EntityPlayer)) return;
		IAffinityData aff = AffinityData.For(caster);
		Set<Affinity> affList = component.getAffinity();
		for (Affinity affinity : affList){
			float shift = component.getAffinityShift(affinity) * aff.getDiminishingReturnsFactor() * 5;
			float xp = 0.05f * aff.getDiminishingReturnsFactor();
			if (governingShape.isChanneled()){
				shift /= 4;
				xp /= 4;
			}

			if (caster instanceof EntityPlayer){
				if (SkillData.For((EntityPlayer)caster).hasSkill(SkillDefs.AFFINITY_GAINS.getID())){
					shift *= 1.1f;
					xp *= 0.9f;
				}
				ItemStack chestArmor = ((EntityPlayer)caster).getItemStackFromSlot(EntityEquipmentSlot.CHEST);
				if (chestArmor != null && ArmorHelper.isInfusionPreset(chestArmor, GenericImbuement.magicXP))
					xp *= 1.25f;
			}

			if (shift > 0){
				AffinityChangingEvent event = new AffinityChangingEvent((EntityPlayer)caster, affinity, shift);
				MinecraftForge.EVENT_BUS.post(event);
				if (!event.isCanceled())
					aff.incrementAffinity(affinity, event.amount);
			}
			if (xp > 0){
				xp *= caster.getAttributeMap().getAttributeInstance(ArsMagicaAPI.xpGainModifier).getAttributeValue();
				EntityExtension.For(caster).addMagicXP(xp);
			}
		}
		aff.addDiminishingReturns(governingShape.isChanneled());
	}
	
//	public static HashMap<Affinity, Float> shiftAffinity(Affinity affinity, float convShift, HashMap<Affinity, Float> hash) {
//		HashMap<Affinity, Float> convMap = new HashMap<Affinity, Float>();
//		float max = 0;
//		for (Entry<Affinity, Float> entry : hash.entrySet()) {
//			if (entry.getValue() == 0 || entry.getKey().equals(affinity))
//				continue;
//			convMap.put(entry.getKey(), entry.getValue());
//			max += entry.getValue();
//		}
//		int num = convMap.size();
//
//		HashMap<Affinity, Float> convCost = new HashMap<Affinity, Float>();
//		convCost.put(affinity, hash.get(affinity));
//		if (num == 0)
//			return convCost;
//		//float shiftPerAffinity = convShift / num;
//		float rest = 0;
//		HashMap<Affinity, Float> costMap = new HashMap<Affinity, Float>();
//		
//		for (Entry<Affinity, Float> entry : convMap.entrySet()) {
//			if (entry.getValue() == 0)
//				continue;
//			float depth = entry.getValue();
//			float percentage = (depth * convShift) / max;
//			//LogHelper.info(percentage);
//			costMap.put(entry.getKey(), percentage);
//		}
//		for (Entry<Affinity, Float> entry : convMap.entrySet()) {
//			if (entry.getValue() == 0)
//				continue;
//			float shiftPerAffinity = costMap.get(entry.getKey());
//			if (entry.getKey().equals(affinity)) {
//				convCost.put(affinity, entry.getValue());
//				continue;
//			}
//			if (entry.getValue() > shiftPerAffinity) {
//				convCost.put(entry.getKey(), entry.getValue() - shiftPerAffinity);
//			} else {
//				convCost.put(entry.getKey(), 0F);
//				rest += shiftPerAffinity - entry.getValue();
//			}
//		}
//		if (rest != 0 && (convCost.containsKey(affinity) ? convCost.size() - 1 != 0 : convCost.size() != 0))
//			return shiftAffinity(affinity, rest, convCost);
//		return convCost;
//	}
//	
//	public static HashMap<Affinity, Float> finalize (Affinity affinity, float convShift, HashMap<Affinity, Float> hash) {
//		if (hash == null)
//			hash = new HashMap<Affinity, Float>();
//		if (hash.containsKey(affinity)) {
//			hash.put(affinity, Math.min(1, (hash.get(affinity) != null ? hash.get(affinity) : 0) + convShift));
//		} else {
//			hash.put(affinity, Math.min(1, convShift));
//		}
//		return hash;
//	}
//	
//	public static void setAffinityData (HashMap<Affinity, Float> map, EntityPlayer player, boolean isChanneled) {
//		for (Affinity aff : ArsMagicaAPI.getAffinityRegistry().getValues()) {
//			if (map.containsKey(aff))
//				AffinityData.For(player).setAffinityDepth(aff, map.get(aff));
//			else
//				AffinityData.For(player).setAffinityDepth(aff, 0.0F);
//		}
//	}
//	
//	private static float finalShift(EntityPlayer player, Affinity aff, float shift) {
//		return AffinityData.For(player).getDiminishingReturnsFactor() * shift;
//	}
//	
//	public static void applyShift (EntityPlayer player, boolean isChanneled, float shift, Affinity toShift) {
//		shift = finalShift(player, toShift, shift);
//		if (!player.worldObj.isRemote)
//			setAffinityData(finalize(toShift, shift, shiftAffinity(toShift, shift, AffinityData.For(player).getAffinities())), player, isChanneled);
//	}
//	
//	public static HashMap<Affinity, Float> getShift(HashMap<Affinity, Float> affinities, float shift, Affinity toShift) {
//		return finalize(toShift, shift, shiftAffinity(toShift, shift, affinities));
//	}
//	
	public static Affinity getMainShiftForStack(ItemStack stack) {
		Affinity aff = Affinity.NONE;
		float maxDepth = 0F;
		HashMap<Affinity, Float> customDepthMap = new HashMap<>();
		ArrayList<SpellComponent> components = SpellUtils.getComponentsForStage(stack, -1);
		for (SpellComponent component : components) {
			for (Affinity aff1 : component.getAffinity()) {
				if (customDepthMap.get(aff1) != null) {
					customDepthMap.put(aff1, customDepthMap.get(aff1) + component.getAffinityShift(aff1));
				} else {
					customDepthMap.put(aff1, component.getAffinityShift(aff1));
				}
			}
		}
		
		for (Entry<Affinity, Float> entry : customDepthMap.entrySet()) {
			if (entry.getValue() > maxDepth) {
				maxDepth = entry.getValue();
				aff = entry.getKey();
			}
		}
		return aff;
	}
	
	public static ItemStack getEssenceForAffinity (Affinity affinity) {
		int meta = 0;
		for (Affinity aff : ArsMagicaAPI.getAffinityRegistry().getValues()) {
			if (aff.equals(affinity))
				break;
			meta++;
		}
		return new ItemStack(ItemDefs.essence, 1, meta);
	}
}
