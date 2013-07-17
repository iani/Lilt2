/* IZ Jul 17, 2013 (12:11 PM) */
/*

To start recording SuperCollider output run this:

Record.start;

To stop recording run this:

Record.stop;

*/


Record {
	*start {
		{
			Server.default.prepareForRecord;
			0.1.wait;
			Server.default.record;
		}.fork;
	}

	*stop { Server.default.stopRecording; }
}
