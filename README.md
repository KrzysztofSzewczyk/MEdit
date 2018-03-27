
<p align="center">
  <img src="https://github.com/KrzysztofSzewczyk/MEdit/blob/master/MEdit.png">
</p>

<a href="https://travis-ci.org/KrzysztofSzewczyk/MEdit"><img src="https://travis-ci.org/KrzysztofSzewczyk/MEdit.svg?branch=master"></a>
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Join the chat at https://gitter.im/MITEdit/Lobby](https://badges.gitter.im/MITEdit/Lobby.svg)](https://gitter.im/MITEdit/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![wercker status](https://app.wercker.com/status/91d3762acc455ca396941f6a1ab63f5d/m/master "wercker status")](https://app.wercker.com/project/byKey/91d3762acc455ca396941f6a1ab63f5d)

MEdit is propably the most advanced Java Swing code editor that is in existence and continous development. Useful links:
 * [JavaDOC](https://krzysztofszewczyk.github.io/MEdit/).
 * [Project WIKI](https://github.com/KrzysztofSzewczyk/MEdit/wiki)
 * [Roadmap](https://github.com/KrzysztofSzewczyk/MEdit/projects)
 * [Issues](https://github.com/KrzysztofSzewczyk/MEdit/issues)
 * [Pull Requests](https://github.com/KrzysztofSzewczyk/MEdit/pulls)

## Features

One of the most important things in text editor are features.
What does MEdit feature? Here's the list:

* Scripting possibility using B++ (You can setup autobuild).
* Own tools using NTS (You can use your favourite compiler with MEdit).
* 100% Pure Java
* Featuring over 90 Programming Languages
* Eight color themes
* Unique syntax highlighing for every language.
* One document per window, multiple windows avaliable to open
* Code folding, bracket matching, animated editing, hyperlink colors, tab lines, matching tags ...
* Code completion (You can set up code completion for any language).
* Regex search and replace
* And MANY MORE!

## Setup

Building this project is really simple, you just need JDK and Eclipse (I'm using
Oxygen.2 one). Just switch workspace to the place where you've cloned repository.
Eclipse should build it automatically, if not you should press build button.
You can make .jar file by doubleclicking jardesc file and following instructions
appearing on screen.

As of now you can use jdk and ant! Check out [this](https://github.com/KrzysztofSzewczyk/MEdit/blob/master/CONTRIBUTING.md) file for information
on building, (and eventually contributing to repository) using ant.

## Dependencies

There are no depedencies for actually building this project (JDK, Eclipse and JRE
should be installed, but it's rather obvious). To make changes to form, you are
recommended to use Eclipse WindowBuilder. If you'd prefer to build it using ANT,
you have to install JDK, JRE, and ANT only.

## Why not GPL?

Why not use GPL (aka the Gnu Virus License)?  Well, there are three
big problems with it.  The first is that if you are a commercial
developer, and have some spare time to contribute to a freeware
product, after spending 10 hours wading through someone else's code,
getting familiar with it, and improving it or bug fixing it, all the
time you spent is wasted, as far as being able to reuse any routines
you found in a commercial product is concerned.  

The second is that encourages others to join the dog-in-the-manger 
brigade.  Someone who ordinarily would be happy to contribute something
to the public domain, once and for all, now instead goes and spends their 
effort on a GPL product, meaning the world still doesn't get the code 
freely available for ALL use (ie in public domain projects AND commercial 
projects, not JUST other GPL projects).

The third is that it is actually technology-inhibitive.  E.g. let's
say there's a GPL wordprocessor, but it doesn't support italics.
Quite a lot of people want italics, but no-one to date has been 
willing to do that work for free.  Let's say a portion of the market
wants italics.  But no one individual can afford to pay the cost of
development by themselves.  Normally this is where a company would
jump in, do the work, and then sell the new version to the market,
meaning that each individual only has to pay a fraction of the
development cost.  But the problem is that the company CAN'T just
make those changes and sell them, because it can't make those
changes proprietary, as it needs to do in order to sell them.  So
instead, the commercial operation needs to develop the entire
equivalent of the GPL wordprocessor, and THEN add italics.  But it
is too expensive for the company to do that, so the technology is
simply never developed!

GPL code will eventually become as useful as public domain code - 50 
years after the death of the original author, when it becomes public 
domain!  That's a long time to have to wait.  Until then, unless your
lawyer informs you that the 2756 license agreement conditions don't 
affect you, the GPL work is only useful as reference material.

Quoted from work of Paul Edwards:
Date:     2007-08-14
Internet: fight.subjugation@gmail.com

Actually, in my opinion GPL can be used to non-reference code - that
means, useless code that author meant to be not-free.

## Misc

This project is my attempt to create useful and full-featured java editor.
Im thinking about it as editor focused on programming, but I might add something
like Eclipse workspaces. GUI library that will be used for development is Java
Swing. 

## New features?

If you would want to create your own feature in MEdit. Check out [this](https://github.com/KrzysztofSzewczyk/MEdit/blob/master/CONTRIBUTING.md) file for information.
If you'd want to add your language support, just follow instructions from the wiki. Be sure
to create a pull request! Please remember, I'm not going to merge PR's that is adding support for language
that has basically no users (is unpopular).
