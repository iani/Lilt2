/* IZ Jul 11, 2013 (4:52 PM) */

BusConnector : Bus {
	var <writers;  // List of (kinds of) ControlConnector or BusBranch instances
	var <readers;  // List of (kinds of) ControlConnector or BusBranch instances

	*control { arg server, numChannels = 1;
		^super.control(server, numChannels).initControl;
	}

	*audio { arg server, numChannels = 1;
		^super.control(server, numChannels).initAudio;
	}

	initControl {
		this.init;
		BusList(server).addControl(this);
	}

	init {
		writers = List();
		readers = List();
	}

	initAudio {
		this.init;
		BusList(server).addAudio(this);
	}

	// Access to readers and writers, busses and connectors
	writerBusses {
		^[this] ++ writers.select({ | w | w.isKindOf(BusBranch) }).collect(_.writerBusConnector)
	}

	readerBusses {
		^[this] ++ readers.select({ | w | w.isKindOf(BusBranch) }).collect(_.readerBusConnector)
	}
}