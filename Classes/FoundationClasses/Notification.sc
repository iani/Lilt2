/*

Enable adding message-specific calls using the changed/update mechanism of Object.

Usage:

anObject.addNotifier(aNotifier, \message, action) :

Make anObject perform action when aNotifier calls:
aNotifier.changed(\message);

Arguments passed to the action are: The extra arguments given in the 'changed' call + the notifier.
The notifier and the message are *not* passed as arguments to the action. This is for several reasons:

- to keep compatibility to NotificationCenter-type calls
- to keep the argument definition part of the action function shorter
- because the notifier that is always passed at the end contains all of:
  - the sender (notifier, changer)
  - the receiver (listener)
  - the message
  - the action.

So all the info is available there.

Example:
(
\listener.addNotifier(\notifier, \test, { | one, two, three, notifier |
	postf("one: %\n", one);
	postf("two: %\n", two);
	postf("three: %\n", three);
	postf("notifier: %\n", notifier);
	notifier.inspect;
});

\notifier.changed(\test, 1, 2, 3);
)

anObject.objectClosed : remove all notifiers and listeners from / to anObject.

Further methods for adding and removing notifiers/listeners are more or less self-explanatory


*/


Notification {
	classvar <all;

	var <notifier, <message, <listener, <>action;

	*initClass {
		all = MultiLevelIdentityDictionary.new;
	}

	*new { | notifier, message, listener, action |
		// First remove previous notification at same address, if it exists:
		this.remove(notifier, message, listener);
		^this.newCopyArgs(notifier, message, listener, action).init;
	}

	init {
		notifier.addDependant(this);
		all.put(notifier, listener, message, this);
	}

	update { | sender, argMessage ... args |
		if (argMessage === message) {
			action.valueArray(if (args.size == 0) { [this] } { args add: this })
		}
	}

	*remove { | notifier, message, listener |
		all.at(notifier, listener, message).remove;
	}

	remove {
		notifier.removeDependant(this);
		all.removeEmptyAt(notifier, listener, message);
	}

	*removeMessage { | message, listener |
		all leafDo: { | path, notification |
			if (notification.message === message and: { notification.listener === listener }) {
				notification.remove;
			}
		}
	}

	*removeListenersOf { | notifier |
		all.leafDoFrom(notifier, { | path, notification |
			notification.notifier.removeDependant(notification);
		});
		all.put(notifier, nil);
	}

	*removeNotifiersOf { | listener |
		all do: { | listenerDict |
			listenerDict keysValuesDo: { | argListener, messageDict |
				if (argListener === listener) { messageDict do: _.remove; }
			}
		}
	}
}

+ Object {
	/* Note: These messages return the receiver, for use in nested
	statements, where the receiver is used for further messages or as
	an argument. To obtain the notification instance, one may construct
	it explicitly: Notification(notifier, message, this, action) */

	addNotifier { | notifier, message, action |
		Notification(notifier, message, this, action);
	}

	removeNotifier { | notifier, message |
		Notification.remove(notifier, message, this);
	}

	// First remove any previous notifier that sends me this message,
	// then add notifier, message
	replaceNotifier { | notifier, message, action |
		this removeMessage: message;
		this.addNotifier(notifier, message, action);
	}

	// remove any notifiers that send me message
	removeMessage { | message |
		Notification.removeMessage(message, this);
	}

	objectClosed {
		this.changed(\objectClosed);
		Notification.removeNotifiersOf(this);
		Notification.removeListenersOf(this);
		this.releaseDependants;
	}

	onObjectClosed { | action |
		this.addDependant({ | changer, changed |
			if (changed === \objectClosed and: { changer === this }) { action.(this) }
		})
	}

	addNotifierOneShot { | notifier, message, action |
		Notification(notifier, message, this, { | ... args |
			action.(*args); //action.(args);
			args.last.remove;
		});
	}

	addNotifierAction { | notifier, message, action |
		var notification;
		notification = Notification.all.at(notifier, message, action);
		if (notification.isNil) {
			this.addNotifier(notifier, message, action);
		}{
			notification.action = notification.action addFunc: action;
		}
	}

	addNotifierSwitch { | switch, message, action, setAction, unsetAction |
		switch.addListener(this, message, action, setAction, unsetAction);
	}

	addNotifierSetActions { | switch, notNilAction, nilAction |
		this.addNotifier(switch, \notifier, { | notifier |
			if (notifier.notNil) { notNilAction.(this, notifier) } { nilAction.(this, notifier) }
		})
	}
}

+ QView {
	releaseOnClose { this.onClose = { this.objectClosed } }

	addNotifier { | notifier, message, action |
		super.addNotifier(notifier, message, action);
		this.releaseOnClose;
	}

}