/* IZ Jul 30, 2013 (6:41 PM

NOTE: Consider using Null-Object pattern for null-notifiers / selected objects

wikipedia.org/wiki/Null_Object_pattern

*/


NotifierSwitch {
	classvar defaultGetNotifierAction;
	var <switcher, <switchMessage, <getGlobalNotifierAction;
	var <>getIndividualNotifierAction;
	var <notifier;
	var <notificationTemplates;
	var <notifications;

	*initClass {
		defaultGetNotifierAction = { | notifier | notifier };
	}

	*new { | switcher, switchMessage = \selected, getGlobalNotifierAction |
		^this.newCopyArgs(switcher, switchMessage,
			getGlobalNotifierAction ? defaultGetNotifierAction, defaultGetNotifierAction
		).init;
	}

	init {
		notificationTemplates = MultiLevelIdentityDictionary();
		this.addNotifier(switcher, switchMessage, { | ... args |
			this.setNotifier(getGlobalNotifierAction.(*args))
		});
	}

	setNotifier { | argNotifier |
		notifications do: _.remove;
		notifications = nil;
		notifier = argNotifier;
		notificationTemplates leafDo: { | path, action |
			this.addNotification(
				action[1].value(notifier, path[0]), path[1], path[0], action[0]
			);
		};
	}

	addNotification { | argNotifier, message, listener, action |
		argNotifier !? {
			notifications = notifications add: Notification(
				argNotifier, message, listener, action
			);
		};
	}

	addListener { | listener, message, action, getNotifierAction |
		var myNotifier;
		listener.removeMessage(message);
		/* The above removes all notifiers on this message for this listener
		   Can we store the previous individual notifier to remove it?
		   However, objects using NotificationSwitch do this to
		   receive a message from one notifier at a time.
		   So it is probably superfluous to do individual removal */
		getNotifierAction = getNotifierAction ? getIndividualNotifierAction;
		notificationTemplates.put(
			listener, message, [action, getNotifierAction];
		);
		this.addNotification(getNotifierAction.(notifier, listener), message, listener, action);
	}

	removeListener { | listener |
		thisMethod.notYetImplemented(this);
	}

	objectClosed {
		notifications do: _.remove;
		super.objectClosed;
	}
}