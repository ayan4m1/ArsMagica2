This is the current list of known bugs.

If you encounter an issue that is not described in this list, please create an issue!

Crashes:

	- Crash with starstrike hitting non-living entities(i haven't been able to produce this but it has been reported)
	- Client crash on spell cast(I have not been able to reproduce this but it has been reported)
	
Bugs:

	- Ender boots do not work when other players are near
	- Random Teleport from Ender Affinity works once, then doesn't work at all until relog
	- Projectile textures and parts of gui get warped
	- Warding candle doesn't update until you swap it out of hand
	- Crafting altar missing texture
	- the /am2 command to set magic level doesn't sync correctly
	- Bosses sometime don't drop anything(there's a check that checks to see if YOU did the last hit, sometimes this check is false when it gets killed)
	- Air Guardian locking servers/clients up with spin attack(it locks the server side)
	- Earth Armor breaks the hand positions
	- Soulbound missing a texture in the Armor Imbuement Table
	- Obelisk rendering is very derpy
	- Summon spell not spawning correct mob
	- Spells in the off-hand not being rendered in the correct position in first-person
	- Spell radius isn't being used properly(if at all)
	- Witchwood forest ignoring blacklist
	- AM2 causing conflicts with Extra Utilities 2's jei implement(not sure how exactly but it has been reported)
	- Tough As Nails not playing nicely, causing parts of its gui to go missing randomly
	- Magician's Workbench not able to recall saved recipes
	- Inscription table can create unusable spells(large amounts of modifiers/components)
	
Performance reports:

	- Mana creepers cause memory leaks
	- Some blocks cause significant performance drops(this is due to the update method being called with unnecessary calls on the client, i plan to restructure them later on to provide an even better performance increase)
