/* IZ Jul 31, 2013 (7:24 PM)

An object that responds to any message by doing nothing: Useful when one wants to send a message to an object but does not know if that object is nil.

NullObject.isRunning;

*/

NullObject {

	*doesNotUnderstand {
		^nil;   /* responding with nil is useful for messages that ask for a value.
		the user of the message can then provide a default value by checking if the
		response was nil */
	}

	// should not use notifiers or dependants etc.
	*addNotifier { ^this }
	*removeNotifier { ^this }
	*replaceNotifier { ^this }
	*removeMessage { ^this }
	*addNotifierOneShot { ^this }
	*addNotifierAction { ^this }
	*addNotifierSwitch { ^this }
}