/* IZ Jul 11, 2013 (4:52 PM) */

BusConnector : Bus {
	var <writers;  // List of (kinds of) ControlConnector or BusBranch instances
	var <readers;  // List of (kinds of) ControlConnector or BusBranch instances

	*control { arg server, numChannels = 1;
		^super.control(server, numChannels).init;
	}

	init {
		writers = List();
		readers = List();
		server.changed(\bus, this);
	}

	writerBusses {
		^[this] ++ writers.select({ | w | w.isKindOf(BusBranch) }).collect(_.writerBusConnector)
	}

	readerBusses {
		^[this] ++ readers.select({ | w | w.isKindOf(BusBranch) }).collect(_.readerBusConnector)
	}
}