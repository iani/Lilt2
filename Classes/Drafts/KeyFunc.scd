/* IZ Aug 6, 2013 (1:38 PM)

Simplify the specification of keyboard commands for GUI widgets.


This is a draft file only for designing the KeyFunc class.


*/

// Using argument order conforming to OSCFunc, MIDIFunc:

KeyFunc({ }, $k); // bind to k key

KeyFunc({ }, $K); // bind to shift-k key

KeyFunc({ }, \esc); // bind to escape key.

Keys represented by symbols are:

\esc -> escape
\escape -> escape
\ret -> return
\return -> return
\bsp -> backspace
\backspace -> backspace
\del -> delete (on mac this is the same as backspace?)
\delete -> delete
\home -> home
\up -> up arrow
\down -> down arrow
\left -> left arrow
\right -> right arrow

(\f1, \f2 ... \f15 ???. Are these caught by the current mechanism?)

Combinations that include modifier keys are coded in a string as follows:

"c-m-a-s-k" -> control, meta (= command), alt, shift, $k

i.e. a string constisting of modifier keys separated by dashes (-) and ending with the non-modifier key of the combination.

only the first letter of each modifier key counts. so:

c = ctl = control = control key
a = alt = alt key
o = opt = option = alt key (option and alt refer to the same key)
m = meta = Command key (apple key or windows key)
s = shift = shift key

So one can write for example:

"c-r" = control-r
"ctl-r" = control-r
"control-r" = control-r
"c-s-r" = control-shift-r
"c-m-r" = control-meta-r
"control-meta-r" = control-meta-r
"ctl-m-r" = control-meta-r

"ctl-m-return" = control-meta-return
"alt-bsp" = alt-backspace
"alt-backspace" = alt-backspace

"opt-left" = option-left arrow key
"alt-left" = option-left arrow key
"o-left" = option-left arrow key
"a-left" = option-left arrow key

etc.

KeyFunc stores the pattern to be matched in a MultiLevelIdentityDictionary:


The first level is the ascii character to be matched
The second level is the numerical code of the modifier combination to be matched
The final level (leaf level) is the function to be performed.

Characters whose ascii and key

Test:

(

w = Window().front;
w.view.keyDownAction = { | v, char, mod, key, other |
	a = a add: char.postln;
	char.ascii.postln;

	mod.postln;
	key.postln;
	other.postln;
};
)

a collect: { | c | c === a[0] };















