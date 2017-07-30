## Return of an old companion
### A simple, configuration-less protection plugin for basic server needs

I've never taken /that/ close of a look at the original, in face i wrote this from the ground up, so please accept (or even better fix and PR) any mistakes I made.

Here's how it works: Place a sign on, above or next to a container/door (at the wall) and write `[private]` in it's first line. You can the write a list of three names below (don't forget your own, please) or leave it empty (will automatically add your name). The first line will automatically turn into `[Lockette]` if the container/door was protected successfully. You can add multiple signs to expand the list of permitted users.

***Be careful: I'm not taking care of pistons right now!***

### For Plugin Devs

You can add invisible PluginLocks to containers and door using the Lockette API, just add Lockette as dependency and take a look at the static methods. Please note: for security reasons you are only allowed to view and edit your plugins PluginLocks.
