Java Fabled Lands (JaFL)
v1.0.6
--------------------------------
An adaptation of the Fabled Lands gamebook series

The most current version of this software (and the source code) can be downloaded from
https://sourceforge.net/projects/flapp/

# Build

## Build the application

Use `make` to build the java application. You will require java install in you computer.

Once the .jar file is created you can execute it with this command:

    java -jar flands.jar

## Build the debian package

To build the debian package use the command:

    gbp buildpackage --no-sign --git-ignore-branch --git-ignore-new

You require the package `default-jdk-headless` to build the package.

# Install

You can install the package running this command:

    # dpkg -i flands_1.0.6~ado1_all.deb

It creates a menu entry in Applications -> Games -> Fabled Lands Game 

# How to use

The aim of this project has been to present an experience as close as possible to reading
the original gamebooks, while automating all the game rules. The main interface ended up
looking a lot like a browser window, with executable elements underlined. Greyed-out text
is disabled; either it will become enabled as you progress through the section, or it
depends on conditions that haven't been met.  Hopefully this is intuitive to use
(if not to explain). If you're not sure what an underlined 'action' will do, you can
right-click on it to bring up a tool-tip, which may or may not clarify the situation.

The rules of the original books can be read in the 'Help' menu. Given that the rules are
implemented for you, the 'Quick Rules' are probably sufficient.

The Adventure Sheet is largely non-interactive. The elements that _are_ interactive
(the item list, money box, blessings, and curses) can be used via the mouse.
Double-clicking will 'use' an item or blessing (wielding a weapon, wearing armour,
using a special item, or using a luck blessing). The items also have popup menus
(activated by right-clicking on an item), through which they can be dropped, sold,
or transferred to a cache. Occasionally you can select multiple items in a list
via a Ctrl + Click, when appropriate.

Abilities are shown in their 'affected' state: that is, their value after all bonuses and
penalties are applied. Each of the six abilities has a tooltip showing how the score has
been calculated; you can see this by bringing the Adventure Sheet to the front (by clicking
the window title-bar) and letting the mouse linger over an ability.

Some book locations contain a 'cache' - a place you can leave items or money.
Double-clicking on an item or money will allow you to transfer stuff between these caches
and your Adventure Sheet. There are also popup context menus which will list
all the options available.

Games are saved to files with the extension '.dat'. The 'Quicksave' and 'Quickload' options
save to and from the file 'savegame.dat'. You can select a saved game in the file browser
to see the character's name, rank, and profession, their location, and the date at which that
game was saved. If this information doesn't come up, that game can't be loaded in
this version.

This version introduces a new 'Hardcore' mode, which deletes a saved game after you've
loaded it and continued on. In this mode, you can also only save the game when you
stop playing. This is an easy system to get around, but I thought some players might
relish the thrill of playing without a safety net.

Each book came with a map of the region it covered; this is the 'local map' displayed in
the bottom right corner of the screen. A 'global map' is available from the menu; most of
the published books are concentrated near the top and center of this map.

The 'Extra Choice' menu usually lists only one option: Death. This will take you to the
section that handles the character's death, and is available because occasionally your
character is killed without an option of what to do next. This may sound nonsensical,
but death is not necessarily an end in this series...

If you survive long enough to own a ship or two, you can transfer crew and cargo between
them by using the 'Ship Transfer' menu item. Note that ships have to be in the same location
before this is enabled. A ship's name can be changed at any time by clicking on the name
and typing. You can also bring up another popup menu for each ship, allowing you to
dump cargo.

The main 'book' font can be changed via the 'Windows->Choose Font...' menu item.

# Credits

All source code is by Jonathan Mann.

Book text copyright Dave Morris & Jamie Thomson, 1996.

Illustrations copyright Russ Nicholson.

Thanks to Andy for creating PDFs of the books. Book 5 was typed up personally, but the
others could be converted far more quickly via cut and paste.

Thanks to Andy and Simon for scanning the book illustrations (I lack scanner-fu).

Thanks to all the bug-finders for improving the game and keeping me honest!

# Bugs

Many, probably. If you discover some, please email me at the address below
or ask for help in the project forums (https://sourceforge.net/projects/flapp/forums)
Changes in the code to deal with newly added books may break books that were previously
working; to find these problems I need to either a) be psychic, or b) have good testers.

A very happy Holy Day of the Recantation of the Soul to all.

_jonathan mann (jemann75@yahoo.com)_

_November '17_
