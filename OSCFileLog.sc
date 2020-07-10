/*
Fork of the class OSCFileLog
from the library : https://github.com/sensestage/SC-OSCFileLog
OSC message recorder adaptation for the Sentire project

*/

OSCFileLog {

	var <recording;
	var <timelogfile;
	var <offset;
	var <oscRecFunc;
	var <>oscPort, <>fn;

	*new{ |fn, oscPort|
		^super.new.init(fn, oscPort);
	}

	init{ |fn, oscPort|
		this.oscPort = oscPort ? 57120;
		this.fn = fn ? "TimeFileLog";

		"initialize SentireOSCFileLog".debug(4, this);
		("fn SentireOSCFileLog" + this.fn).debug(4, this);
		("oscPort SentireOSCFileLog" + this.oscPort).debug(4, this);

		this.open(this.fn);
	}

	open{ |filename|
		("filename:" + filename).debug(4, this);
		this.fn = filename;

		timelogfile = MultiFileWriter.new( filename ).zipSingle_( false ).tarBundle_( false );
		timelogfile.open;
		thisProcess.openUDPPort(this.oscPort);

		recording = true;

	}

	writeOSC { | oscPath |

		thisProcess.removeOSCRecvFunc( oscRecFunc );

		("oscPath" + oscPath).debug(4, this);

		oscRecFunc = { |msg, time, replyAddr, recvPort|
			("oscRecFunc msg: " ++ msg).debug(6, this); // this generates a lot of output, that's why I set a really high ~debugLevel

			if( recvPort == this.oscPort && oscPath.find([msg[0].asString]).notNil, {
				this.writeLine( time, msg[0], msg.copyToEnd( 1 ) );
			});

		};

		this.resetTime;
		thisProcess.addOSCRecvFunc( oscRecFunc );
		("recording OSC data to " ++ timelogfile).debug(4, this);
		// ("recording OSC data to " ++ timelogfile.curfn).debug(4, this);

	}

	writeLine{ |time,tag,data|
		timelogfile.writeLine( [time - offset, tag.asCompileString ] ++ data.collect{ |it| it } );
	}

	resetTime{
		offset = Process.elapsedTime;
	}

	close{
		thisProcess.removeOSCRecvFunc( oscRecFunc );
		recording = false;
		timelogfile.close;
	}
}



