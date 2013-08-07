/* IZ Aug 6, 2013 (7:16 PM)

======= Instance creation arguments: ========

- switcher
Object that sends the switchMessage to set a new message to listen to.

- switchMessage:
The message that changes the message to listen to. Sent by the switcher.

- listener:
Object that receives the messages and acts. (Like the listener of a Notification)

- switchAction:
Function evaluated when the chosen message changes. Usually serves 2 purposes:
- Create a custom version for updating the value of the listener when message is received.
  In the case of sliders or knobs, this involves fetching the spec from the notifier for unmapping the value.
- Update the value of the listener.

- action:
Function evaluated whenever the chosen message is received. Functions like the action of a Notification.


========== Usage scenario / example ==========

Consider a gui for controlling a synth's parameters with sliders. To save screen-space the gui has less sliders than the synth has parameters. Instead, the user can choose which parameter is controlled by each slider through a menu. The case described here is the simplest one, consisting of one menu and one slider. The menu lists the names of the parameters of the synth. When the user chooses one of the parameters in the menu, the slider will start controlling that parameter. For example, the menu may have just two items 'amp' and 'freq', corresponding to the 'amp' and 'freq' parameters of the synth. When the user chooses 'amp', the slider starts controlling the parameter 'amp', when the user chooses 'freq', the slider starts controlling the parameter 'freq'.  The implementation with MessageSwitch looks like this:

slider.addMessageSwitch(
menu,
\parameter,
{ | value, notification | notification.listener.value = value },
{ | message, notifier, listener | listener.value = notifier.at(message) }
)

The switcher becomes a Notification with:
- notifier: The menu
- message: The switchMessage (default: \parameter)
- listener: The slider
- action: A custom function which does for the MessageSwitch:
  - set the notifier of itself from an argument received from the notifier
  - set the message of itself from an argument received from the notifier
  - add itself (!) as dependant to the notifier. As dependant, it will
    evaluate action upon update, only if the changed aspect matches message.
  - evaluate switchAction with arguments itself.
    The switchAction can then create the action function for the MessageSwitch
    as well as for the listener, for example like this:

{ | switch, notifier, message, listener |
  var spec;
  spec = notifier.makeSpec(message);
  switch.action = { | value | listener.value = spec.unmap(value) };
  listener.action = { | me | notifier.put(message, spec.map(me.value) };
}

The above function can be stored as template in a method of MessageSwitch:
  mappedParamAction

And analogous to this a function for NumberBox: simpleParamAction

TODO: ensure that all relevant dependants and notifications are removed when either the switcher, or the notifier or the listener close.

*/

MessageSwitch {
	var <switcher, <>switchMessage, <listener, <switchAction, <>action, <notifier, <message;

	*new { | switcher, switchMessage, listener, switchAction, action |
		^this.newCopyArgs(switcher, switchMessage, listener, switchAction, action).init;
	}


}

+ Object {

	addMessageSwitch { | switcher, switchMessage, switchAction, action |
		MessageSwitch(switcher, switchMessage, this, switchAction, action);
	}
}