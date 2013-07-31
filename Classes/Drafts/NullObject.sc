/* IZ Jul 31, 2013 (7:24 PM)

An object that responds to any message by doing nothing: Useful when one wants to send a message to an object but does not know if that object is nil.

NullObject.isRunning;

*/

NullObject {

	*doesNotUnderstand {
		// Return self. Enables chaining of messages:
		// NullObject.aMessage.anotherMessage
	}

	// behave like nil in queries:

	? { arg obj; ^obj }
	?? { arg obj; ^obj.value(this) }
	!? { arg obj; ^this }

	*isNil { ^true }
	*notNil { ^false }

	// should not use notifiers or dependants etc.
	*addNotifier { ^this }
	*removeNotifier { ^this }
	*replaceNotifier { ^this }
	*removeMessage { ^this }
	*addNotifierOneShot { ^this }
	*addNotifierAction { ^this }
	*addNotifierSwitch { ^this }
}