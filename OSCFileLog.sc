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

// reads an oscfilelog and plays it

OSCFileLogPlayer{
	var <reader;
	var playTask;

//	var <timeMap;
	var <curTime=0;
//	var <deltaT=0;

	var <fileClass;
	// var <hasStamp = false;
	// var <hasExtraTab = false;

	var <targetaddr;

	*new{ |fn,addr|
		^super.new.init( fn, addr );
	}

	init{ |fn,addr|
		targetaddr = addr;
		this.checkFileClass( fn );
		this.open( fn );
	}

	checkFileClass{ |fn|
		var tar,txt;
		var path = PathName(fn);
		tar = (path.extension == "tar");
		txt = (path.extension == "txt");
		if ( tar ){
			fileClass = MultiFilePlayer;
		}{
			if ( txt ){
				fileClass = TabFilePlayer;
			}{
				fileClass = MultiFilePlayer;
			}
		};
		//		[tar, txt, fileClass].postln;
	}

	open{ |fn|
		if ( playTask.notNil ){ playTask.stop; };
		if ( reader.notNil ){ reader.close; };

		reader = fileClass.new( fn );

	//	this.readHeader;

		playTask = Task{
			var dt = 0;
			while( { dt.notNil }, {
				dt = this.readLine;
				if ( dt.notNil ){
					dt.wait;
				}
			});
		};
	}

	readLine{ |update=true|
		var dt,line,data;
		var oldid;
		var oldTime = curTime;
		oldid = reader.curid;
		line = reader.nextInterpret;
		line.postcs;
		// header may have changed:
	//	if ( oldid != reader.curid ){
	//		this.readHeader;
	//	};
		if ( line.isNil ){
			"At end of data".postln;
			^nil;
		};
		curTime = line.first;
		if ( update ){
			targetaddr.sendMsg( *line.copyToEnd( 1 ) );
		};
		dt = curTime - oldTime;
		^dt;
	}


	play{ |clock|
		playTask.start( clock );
	}

	pause{
		playTask.pause;
	}

	resume{
		playTask.resume;
	}

	stop{
		playTask.stop;
		this.reset;
	}

	reset{
		curTime = 0;
		reader.reset;
//		this.readHeader;
		playTask.reset;
	}

	close{
		playTask.stop;
		reader.close;
	}

		/*
	goToTime{ |newtime|
		var line,oldid;
		if ( deltaT == 0 ){
			deltaT = this.readLine;
		};
		line = floor( newtime / deltaT );
		curTime = line * deltaT;
		// assuming dt is constant.
		if ( fileClass == MultiFilePlayer ){
			oldid = reader.curid;
			reader.goToLine( line.asInteger );
			// header may have changed:
			if ( oldid != reader.curid ){
				this.readHeader;
			};
		}{
			reader.goToLine( line.asInteger );
		};
	}
*/

/*
	readHeader{
		var spec,playset,playids;
		var playslots;
		var header;
		playnodes = Dictionary.new;
		header = reader.readHeader(hs:2);
		spec = header[0].last;
		if ( spec.notNil, {
			network.setSpec( spec );
			// if spec was not local, it may be included in the tar-ball
			if ( network.spec.isNil ){
				reader.extractFromTar( spec ++ ".spec" );
				network.spec.fromFileName( reader.pathDir +/+ spec );
			};
		});
		playslots = header[1].drop(1).collect{ |it| it.interpret };
		if ( fileClass == TabFilePlayer ){
			// backwards compatibility (there was an extra tab written at the end)
			playslots = playslots.drop(-1);
			hasExtraTab = true;
		};
		if ( playslots.first == "time" ){
			// date stamps in the first column:
			playslots.drop(1);
			hasStamp = true;
		};
		playset = Set.new;
		playids = playslots.collect{ |it| it.first }.do{
			|it,i| playset.add( it );
		};
		playset.do{ |it|
			network.addExpected( it );
			playnodes.put( it, Array.new )
		};
		playids.do{ |it,i|
			playnodes.put( it, playnodes[it].add( i ) )
		};
	}
*/
}

