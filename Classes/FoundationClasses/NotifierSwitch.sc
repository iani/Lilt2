/* IZ Jul 30, 2013 (6:41 PM


NOTE: Consider using Null-Object pattern instead of 2 sets of actions

wikipedia.org/wiki/Null_Object_pattern



*/


NotifierSwitch {
	var <switcher, <switchMessage, <getNotifierAction;
	var <notifier;
	var <notificationTemplates; // multilevel dict of listener objects, messages, actions
	var <notifications;

	// TODO: adding and removing of switchSetActions;
	var <switchSetActions;
	var <switchUnsetActions;

	*new { | switcher, switchMessage, getNotifierAction |
		^this.newCopyArgs(switcher, switchMessage, getNotifierAction).init;
	}

	init {
		notificationTemplates = MultiLevelIdentityDictionary();
		getNotifierAction ?? { getNotifierAction = { | notifier | notifier } };
		this.addNotifier(switcher, switchMessage, { | ... args |
			this.setNotifier(getNotifierAction.(*args))
		});
	}

	setNotifier { | argNotifier |
		notifications do: _.remove;
		notifications = nil;
		notifier = argNotifier;
		if (notifier.isNil) {
			switchUnsetActions do: _.value;
		}{
			switchSetActions do: _.(notifier);
			notificationTemplates leafDo: { | path, action |
				this.addNotification(notifier, path[1], path[0], action);
			}
		};

	}

	addNotification { | argNotifier, message, listener, action |
		notifications = notifications add: Notification(
			argNotifier, message, listener, action
		);
	}

	addListener { | listener, message, action, setAction, unsetAction |
		notificationTemplates.put(listener, message, action);
		if (notifier.isNil) {
			unsetAction.value;
		}{
			setAction.(notifier);
			listener.removeNotifier(notifier, message);
			this.addNotification(notifier, message, listener, action);
		};
	}

	removeListener { | listener |
		thisMethod.notYetImplemented(this);
	}

	objectClosed {
		notifications do: _.remove;
		super.objectClosed;
	}
}