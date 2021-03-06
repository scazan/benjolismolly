// CroneEngine_BenjolisMolly
// Classic polysynth with a Juno-6 voice structure, the extra modulation of a Jupiter-8, and CS-80 inspired ring modulation.
// v1.0.2 Mark Eats

Engine_BenjolisMolly : CroneEngine {

  // Molly The Poly
	classvar maxNumVoices = 10;
	var voiceGroup;
	var voiceList;
	var lfo;
	var mixer;

	var lfoBus;
	var ringModBus;
	var mixerBus;

	var pitchBendRatio = 1;

	var oscWaveShape = 0;
	var pwMod = 0;
	var pwModSource = 0;
	var freqModLfo = 0;
	var freqModEnv = 0;
	var lastFreq = 0;
	var glide = 0;
	var mainOscLevel = 1;
	var subOscLevel = 0;
	var subOscDetune = 0;
	var noiseLevel = 0;
	var hpFilterCutoff = 10;
	var lpFilterType = 0;
	var lpFilterCutoff = 440;
	var lpFilterResonance = 0.2;
	var lpFilterCutoffEnvSelect = 0;
	var lpFilterCutoffModEnv = 0;
	var lpFilterCutoffModLfo = 0;
	var lpFilterTracking = 1;
	var lfoFade = 0;
	var env1Attack = 0.01;
	var env1Decay = 0.3;
	var env1Sustain = 0.5;
	var env1Release = 0.5;
	var env2Attack = 0.01;
	var env2Decay = 0.3;
	var env2Sustain = 0.5;
	var env2Release = 0.5;
	var ampMod = 0;
	var channelPressure = 0;
	var timbre = 0;
	var ringModFade = 0;
	var ringModMix = 0;
	var chorusMix = 0;
	var voiceHold = false;

  // Benjolis
  var benjolisSynth;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {

		voiceGroup = Group.new(context.xg);
		voiceList = List.new();

		lfoBus = Bus.control(context.server, 1);
		ringModBus = Bus.audio(context.server, 1);
		mixerBus = Bus.audio(context.server, 1);


		// Synth voice
		SynthDef(\voice, {
			arg out, lfoIn, ringModIn, freq = 440, pitchBendRatio = 1, gate = 0, killGate = 1, vel = 1, pressure, timbre,
			oscWaveShape, pwMod, pwModSource, freqModLfo, freqModEnv, lastFreq, glide, mainOscLevel, subOscLevel, subOscDetune, noiseLevel,
			hpFilterCutoff, lpFilterCutoff, lpFilterResonance, lpFilterType, lpFilterCutoffEnvSelect, lpFilterCutoffModEnv, lpFilterCutoffModLfo, lpFilterTracking,
			lfoFade, env1Attack, env1Decay, env1Sustain, env1Release, env2Attack, env2Decay, env2Sustain, env2Release,
			ampMod, ringModFade, ringModMix;
			var i_nyquist = SampleRate.ir * 0.5, i_cFreq = 48.midicps, signal, killEnvelope, controlLag = 0.005,
			lfo, ringMod, oscArray, freqModRatio, mainOscDriftLfo, subOscDriftLfo, filterCutoffRatio, filterCutoffModRatio,
			envelope1, envelope2;

			// LFO in
			lfo = Line.kr(start: (lfoFade < 0), end: (lfoFade >= 0), dur: lfoFade.abs, mul: In.kr(lfoIn, 1));
			ringMod = Line.kr(start: (ringModFade < 0), end: (ringModFade >= 0), dur: ringModFade.abs, mul: In.ar(ringModIn, 1));

			// Lag and map inputs

			freq = XLine.kr(start: lastFreq, end: freq, dur: glide + 0.001);
			freq = Lag.kr(freq * pitchBendRatio, 0.005);
			pressure = Lag.kr(pressure, controlLag);

			pwMod = Lag.kr(pwMod, controlLag);
			mainOscLevel = Lag.kr(mainOscLevel, controlLag);
			subOscLevel = Lag.kr(subOscLevel, controlLag);
			subOscDetune = Lag.kr(subOscDetune, controlLag);
			noiseLevel = Lag.kr(noiseLevel, controlLag);

			hpFilterCutoff = Lag.kr(hpFilterCutoff, controlLag);
			lpFilterCutoff = Lag.kr(lpFilterCutoff, controlLag);
			lpFilterResonance = Lag.kr(lpFilterResonance, controlLag);
			lpFilterType = Lag.kr(lpFilterType, 0.01);

			ringModMix = Lag.kr((ringModMix + timbre).clip, controlLag);

			// Envelopes
			killGate = killGate + Impulse.kr(0); // Make sure doneAction fires
			killEnvelope = EnvGen.kr(envelope: Env.asr( 0, 1, 0.01), gate: killGate, doneAction: Done.freeSelf);

			envelope1 = EnvGen.ar(envelope: Env.adsr( env1Attack, env1Decay, env1Sustain, env1Release), gate: gate);
			envelope2 = EnvGen.ar(envelope: Env.adsr( env2Attack, env2Decay, env2Sustain, env2Release), gate: gate, doneAction: Done.freeSelf);

			// Main osc

			// Note: Would be ideal to do this exponentially but its a surprisingly big perf hit
			freqModRatio = ((lfo * freqModLfo) + (envelope1 * freqModEnv));
			freqModRatio = Select.ar(freqModRatio >= 0, [
				freqModRatio.linlin(-2, 0, 0.25, 1),
				freqModRatio.linlin(0, 2, 1, 4)
			]);
			freq = (freq * freqModRatio).clip(20, i_nyquist);

			mainOscDriftLfo = LFNoise2.kr(freq: 0.1, mul: 0.001, add: 1);

			pwMod = Select.kr(pwModSource, [lfo.range(0, pwMod), envelope1 * pwMod, pwMod]);

			oscArray = [
				VarSaw.ar(freq * mainOscDriftLfo),
				Saw.ar(freq * mainOscDriftLfo),
				Pulse.ar(freq * mainOscDriftLfo, width: 0.5 + (pwMod * 0.49)),
			];
			signal = Select.ar(oscWaveShape, oscArray) * mainOscLevel;

			// Sub osc and noise
			subOscDriftLfo = LFNoise2.kr(freq: 0.1, mul: 0.0008, add: 1);
			signal = SelectX.ar(subOscLevel * 0.5, [signal, Pulse.ar(freq * 0.5 * subOscDetune.midiratio * subOscDriftLfo, width: 0.5)]);
			signal = SelectX.ar(noiseLevel * 0.5, [signal, WhiteNoise.ar()]);
			signal = signal + PinkNoise.ar(0.007);

			// HP Filter
			filterCutoffRatio = Select.kr((freq < i_cFreq), [
				i_cFreq + (freq - i_cFreq),
				i_cFreq - (i_cFreq - freq)
			]);
			filterCutoffRatio = filterCutoffRatio / i_cFreq;
			hpFilterCutoff = (hpFilterCutoff * filterCutoffRatio).clip(10, 20000);
			signal = HPF.ar(in: signal, freq: hpFilterCutoff);

			// LP Filter
			filterCutoffRatio = Select.kr((freq < i_cFreq), [
				i_cFreq + ((freq - i_cFreq) * lpFilterTracking),
				i_cFreq - ((i_cFreq - freq) * lpFilterTracking)
			]);
			filterCutoffRatio = filterCutoffRatio / i_cFreq;
			lpFilterCutoff = lpFilterCutoff * (1 + (pressure * 0.55));
			lpFilterCutoff = lpFilterCutoff * filterCutoffRatio;

			// Note: Again, would prefer this to be exponential
			filterCutoffModRatio = ((lfo * lpFilterCutoffModLfo) + ((Select.ar(lpFilterCutoffEnvSelect, [envelope1, envelope2]) * lpFilterCutoffModEnv) * 2));
			filterCutoffModRatio = Select.ar(filterCutoffModRatio >= 0, [
				filterCutoffModRatio.linlin(-3, 0, 0.08333333333, 1),
				filterCutoffModRatio.linlin(0, 3, 1, 12)
			]);
			lpFilterCutoff = (lpFilterCutoff * filterCutoffModRatio).clip(20, 20000);

			signal = RLPF.ar(in: signal, freq: lpFilterCutoff, rq: lpFilterResonance.linexp(0, 1, 1, 0.05));
			signal = SelectX.ar(lpFilterType, [signal, RLPF.ar(in: signal, freq: lpFilterCutoff, rq: lpFilterResonance.linexp(0, 1, 1, 0.32))]);

			// Amp
			signal = signal * envelope2 * killEnvelope;
			signal = signal * pressure * lfo.range(1 - ampMod, 1);
			// signal = signal * (1 + (pressure * 1.15));


			// Ring mod
			signal = SelectX.ar(ringModMix * 0.5, [signal, signal * ringMod]);

			Out.ar(out, signal);
		}).add;


		// LFO
		lfo = SynthDef(\lfo, {
			arg lfoOut, ringModOut, lfoFreq = 5, lfoWaveShape = 0, ringModFreq = 50;
			var lfo, lfoOscArray, ringMod, controlLag = 0.005;

			// Lag inputs
			lfoFreq = Lag.kr(lfoFreq, controlLag);
			ringModFreq = Lag.kr(ringModFreq, controlLag);

			lfoOscArray = [
				SinOsc.kr(lfoFreq),
				LFTri.kr(lfoFreq),
				LFSaw.kr(lfoFreq),
				LFPulse.kr(lfoFreq, mul: 2, add: -1),
				LFNoise0.kr(lfoFreq)
			];

			lfo = Select.kr(lfoWaveShape, lfoOscArray);
			lfo = Lag.kr(lfo, 0.005);

			Out.kr(lfoOut, lfo);

			ringMod = SinOsc.ar(ringModFreq);
			Out.ar(ringModOut, ringMod);

		}).play(target:context.xg, args: [\lfoOut, lfoBus, \ringModOut, ringModBus], addAction: \addToHead);


		// Mixer and chorus
		mixer = SynthDef(\mixer, {
			arg in, out, amp = 0.5, chorusMix = 0;
			var signal, chorus, chorusPreProcess, chorusLfo, chorusPreDelay = 0.01, chorusDepth = 0.0053, chorusDelay, controlLag = 0.005;

			// Lag inputs
			amp = Lag.kr(amp, controlLag);
			chorusMix = Lag.kr(chorusMix, controlLag);

			signal = In.ar(in, 1) * 0.4 * amp;

			// Bass boost
			signal = BLowShelf.ar(signal, freq: 400, rs: 1, db: 2, mul: 1, add: 0);

			// Compression etc
			signal = LPF.ar(in: signal, freq: 14000);
			signal = CompanderD.ar(in: signal, thresh: 0.4, slopeBelow: 1, slopeAbove: 0.25, clampTime: 0.002, relaxTime: 0.01);
			signal = tanh(signal).softclip;

			// Chorus

			chorusPreProcess = signal + (signal * WhiteNoise.ar(0.004));

			chorusLfo = LFPar.kr(chorusMix.linlin(0.7, 1, 0.5, 0.75));
			chorusDelay = chorusPreDelay + chorusMix.linlin(0.5, 1, chorusDepth, chorusDepth * 0.75);

			chorus = Array.with(
				DelayC.ar(in: chorusPreProcess, maxdelaytime: chorusPreDelay + chorusDepth, delaytime: chorusLfo.range(chorusPreDelay, chorusDelay)),
				DelayC.ar(in: chorusPreProcess, maxdelaytime: chorusPreDelay + chorusDepth, delaytime: chorusLfo.range(chorusDelay, chorusPreDelay))
			);
			chorus = LPF.ar(chorus, 14000);

			Out.ar(bus: out, channelsArray: SelectX.ar(chorusMix * 0.5, [signal.dup, chorus]));

		}).play(target:context.xg, args: [\in, mixerBus, \out, context.out_b], addAction: \addToTail);


		// Commands

		// noteOn(id, freq, vel)
		this.addCommand(\noteOn, "iff", { arg msg;

			var id = msg[1], freq = msg[2], vel = msg[3];
			var voiceToRemove, newVoice;

			// Remove voice if ID matches or there are too many
			voiceToRemove = voiceList.detect{arg item; item.id == id};
			if(voiceToRemove.isNil && (voiceList.size >= maxNumVoices), {
				voiceToRemove = voiceList.detect{arg v; v.gate == 0};
				if(voiceToRemove.isNil, {
					voiceToRemove = voiceList.last;
				});
			});
			if(voiceToRemove.notNil, {
				voiceToRemove.theSynth.set(\gate, 0);
				voiceToRemove.theSynth.set(\killGate, 0);
				voiceList.remove(voiceToRemove);
			});

			if(lastFreq == 0, {
				lastFreq = freq;
			});

			// Add new voice
			context.server.makeBundle(nil, {
				newVoice = (id: id, theSynth: Synth.new(defName: \voice, args: [
					\out, mixerBus,
					\lfoIn, lfoBus,
					\ringModIn, ringModBus,
					\freq, freq,
					\pitchBendRatio, pitchBendRatio,
					\gate, 1,
					\vel, vel.linlin(0, 1, 0.3, 1),
					\pressure, channelPressure,
					\timbre, timbre,
					\oscWaveShape, oscWaveShape,
					\pwMod, pwMod,
					\pwModSource, pwModSource,
					\freqModLfo, freqModLfo,
					\freqModEnv, freqModEnv,
					\lastFreq, lastFreq,
					\glide, glide,
					\mainOscLevel, mainOscLevel,
					\subOscLevel, subOscLevel,
					\subOscDetune, subOscDetune,
					\noiseLevel, noiseLevel,
					\hpFilterCutoff, hpFilterCutoff,
					\lpFilterType, lpFilterType,
					\lpFilterCutoff, lpFilterCutoff,
					\lpFilterResonance, lpFilterResonance,
					\lpFilterCutoffEnvSelect, lpFilterCutoffEnvSelect,
					\lpFilterCutoffModEnv, lpFilterCutoffModEnv,
					\lpFilterCutoffModLfo, lpFilterCutoffModLfo,
					\lpFilterTracking, lpFilterTracking,
					\lfoFade, lfoFade,
					\env1Attack, env1Attack,
					\env1Decay, env1Decay,
					\env1Sustain, env1Sustain,
					\env1Release, env1Release,
					\env2Attack, env2Attack,
					\env2Decay, env2Decay,
					\env2Sustain, env2Sustain,
					\env2Release, env2Release,
					\ampMod, ampMod,
					\ringModFade, ringModFade,
					\ringModMix, ringModMix
				], target: voiceGroup).onFree({ voiceList.remove(newVoice); }), gate: 1);

				voiceList.addFirst(newVoice);
				lastFreq = freq;
			});
		});

		// noteOff(id)
    this.addCommand(\noteOff, "i", { arg msg;
      if (voiceHold != true, {
        var voice = voiceList.detect{arg v; v.id == msg[1]};
        if(voice.notNil, {
          voice.theSynth.set(\gate, 0);
          voice.gate = 0;
        });
      });
		});

		// noteOffAll()
		this.addCommand(\noteOffAll, "", { arg msg;
			voiceGroup.set(\gate, 0);
			voiceList.do({ arg v; v.gate = 0; });
		});

		// noteKill(id)
		this.addCommand(\noteKill, "i", { arg msg;
			var voice = voiceList.detect{arg v; v.id == msg[1]};
			if(voice.notNil, {
				voice.theSynth.set(\gate, 0);
				voice.theSynth.set(\killGate, 0);
				voiceList.remove(voice);
			});
		});

		// noteKillAll()
		this.addCommand(\noteKillAll, "", { arg msg;
			voiceGroup.set(\gate, 0);
			voiceGroup.set(\killGate, 0);
			voiceList.clear;
		});

    this.addCommand(\noteHold, "i", { arg msg;
      if (msg[1].asInteger == 1, {
        voiceHold = true;
      }, {
        voiceHold = false;
      });
    });

    // pitchBend(id, ratio)
    this.addCommand(\pitchBend, "if", { arg msg;
			var voice = voiceList.detect{arg v; v.id == msg[1]};
			if(voice.notNil, {
				voice.theSynth.set(\pitchBendRatio, msg[2]);
			});
		});

		// pitchBendAll(ratio)
		this.addCommand(\pitchBendAll, "f", { arg msg;
			pitchBendRatio = msg[1];
			voiceGroup.set(\pitchBendRatio, pitchBendRatio);
		});

		// pressure(id, pressure)
		this.addCommand(\pressure, "if", { arg msg;
			var voice = voiceList.detect{arg v; v.id == msg[1]};
			if(voice.notNil, {
				voice.theSynth.set(\pressure, msg[2]);
			});
		});

		// pressureAll(pressure)
		this.addCommand(\pressureAll, "f", { arg msg;
      if (voiceHold != true, {
        channelPressure = msg[1];
        voiceGroup.set(\pressure, channelPressure);
      });
		});

		// timbre(id, timbre)
		this.addCommand(\timbre, "if", { arg msg;
			var voice = voiceList.detect{arg v; v.id == msg[1]};
			if(voice.notNil, {
				voice.theSynth.set(\timbre, msg[2]);
			});
		});

		// timbreAll(timbre)
		this.addCommand(\timbreAll, "f", { arg msg;
			timbre = msg[1];
			voiceGroup.set(\timbre, timbre);
		});


		this.addCommand(\oscWaveShape, "i", { arg msg;
			oscWaveShape = msg[1];
			voiceGroup.set(\oscWaveShape, oscWaveShape);
		});

    this.addCommand(\pwMod, "f", { arg msg;
      pwMod = msg[1];
      voiceGroup.set(\pwMod, pwMod);
    });

		this.addCommand(\pwModSource, "i", { arg msg;
			pwModSource = msg[1];
			voiceGroup.set(\pwModSource, pwModSource);
		});

		this.addCommand(\freqModLfo, "f", { arg msg;
			freqModLfo = msg[1];
			voiceGroup.set(\freqModLfo, freqModLfo);
		});

		this.addCommand(\freqModEnv, "f", { arg msg;
			freqModEnv = msg[1];
			voiceGroup.set(\freqModEnv, freqModEnv);
		});

		this.addCommand(\glide, "f", { arg msg;
			glide = msg[1];
			voiceGroup.set(\glide, glide);
		});

		this.addCommand(\mainOscLevel, "f", { arg msg;
			mainOscLevel = msg[1];
			voiceGroup.set(\mainOscLevel, mainOscLevel);
		});

		this.addCommand(\subOscLevel, "f", { arg msg;
			subOscLevel = msg[1];
			voiceGroup.set(\subOscLevel, subOscLevel);
		});

		this.addCommand(\subOscDetune, "f", { arg msg;
			subOscDetune = msg[1];
			voiceGroup.set(\subOscDetune, subOscDetune);
		});

		this.addCommand(\noiseLevel, "f", { arg msg;
			noiseLevel = msg[1];
			voiceGroup.set(\noiseLevel, noiseLevel);
		});

		this.addCommand(\hpFilterCutoff, "f", { arg msg;
			hpFilterCutoff = msg[1];
			voiceGroup.set(\hpFilterCutoff, hpFilterCutoff);
		});

		this.addCommand(\lpFilterType, "i", { arg msg;
			lpFilterType = msg[1];
			voiceGroup.set(\lpFilterType, lpFilterType);
		});

		this.addCommand(\lpFilterCutoff, "f", { arg msg;
			lpFilterCutoff = msg[1];
			voiceGroup.set(\lpFilterCutoff, lpFilterCutoff);
		});

		this.addCommand(\lpFilterResonance, "f", { arg msg;
			lpFilterResonance = msg[1];
			voiceGroup.set(\lpFilterResonance, lpFilterResonance);
		});

		this.addCommand(\lpFilterCutoffEnvSelect, "i", { arg msg;
			lpFilterCutoffEnvSelect = msg[1];
			voiceGroup.set(\lpFilterCutoffEnvSelect, lpFilterCutoffEnvSelect);
		});

		this.addCommand(\lpFilterCutoffModEnv, "f", { arg msg;
			lpFilterCutoffModEnv = msg[1];
			voiceGroup.set(\lpFilterCutoffModEnv, lpFilterCutoffModEnv);
		});

		this.addCommand(\lpFilterCutoffModLfo, "f", { arg msg;
			lpFilterCutoffModLfo = msg[1];
			voiceGroup.set(\lpFilterCutoffModLfo, lpFilterCutoffModLfo);
		});

		this.addCommand(\lpFilterTracking, "f", { arg msg;
			lpFilterTracking = msg[1];
			voiceGroup.set(\lpFilterTracking, lpFilterTracking);
		});

		this.addCommand(\lfoFade, "f", { arg msg;
			lfoFade = msg[1];
			voiceGroup.set(\lfoFade, lfoFade);
		});

		this.addCommand(\env1Attack, "f", { arg msg;
			env1Attack = msg[1];
			voiceGroup.set(\env1Attack, env1Attack);
		});

		this.addCommand(\env1Decay, "f", { arg msg;
			env1Decay = msg[1];
			voiceGroup.set(\env1Decay, env1Decay);
		});

		this.addCommand(\env1Sustain, "f", { arg msg;
			env1Sustain = msg[1];
			voiceGroup.set(\env1Sustain, env1Sustain);
		});

		this.addCommand(\env1Release, "f", { arg msg;
			env1Release = msg[1];
			voiceGroup.set(\env1Release, env1Release);
		});

		this.addCommand(\env2Attack, "f", { arg msg;
			env2Attack = msg[1];
			voiceGroup.set(\env2Attack, env2Attack);
		});

		this.addCommand(\env2Decay, "f", { arg msg;
			env2Decay = msg[1];
			voiceGroup.set(\env2Decay, env2Decay);
		});

		this.addCommand(\env2Sustain, "f", { arg msg;
			env2Sustain = msg[1];
			voiceGroup.set(\env2Sustain, env2Sustain);
		});

		this.addCommand(\env2Release, "f", { arg msg;
			env2Release = msg[1];
			voiceGroup.set(\env2Release, env2Release);
		});

		this.addCommand(\ampMod, "f", { arg msg;
			ampMod = msg[1];
			voiceGroup.set(\ampMod, ampMod);
		});

		this.addCommand(\ringModFade, "f", { arg msg;
			ringModFade = msg[1];
			voiceGroup.set(\ringModFade, ringModFade);
		});

		this.addCommand(\ringModMix, "f", { arg msg;
			ringModMix = msg[1];
			voiceGroup.set(\ringModMix, ringModMix);
		});

		this.addCommand(\amp, "f", { arg msg;
			mixer.set(\amp, msg[1]);
		});

		this.addCommand(\chorusMix, "f", { arg msg;
			mixer.set(\chorusMix, msg[1]);
		});

		this.addCommand(\lfoFreq, "f", { arg msg;
			lfo.set(\lfoFreq, msg[1]);
		});

		this.addCommand(\lfoWaveShape, "i", { arg msg;
			lfo.set(\lfoWaveShape, msg[1]);
		});

		this.addCommand(\ringModFreq, "f", { arg msg;
			lfo.set(\ringModFreq, msg[1]);
		});



    // BENJOLIS
    SynthDef.new(\benjolis, {
            | out, freq1= 40, freq2=4, scale=1, rungler1=0.16, rungler2=0.0, runglerFilt=9, loop=0, filtFreq=40, q=0.82, gain=1, filterType=0, outSignal=6, amp=0, pan=0|
            var osc1, osc2, tri1, tri2, sh0, sh1, sh2, sh3, sh4, sh5, sh6, sh7, sh8=1, rungler, pwm, filt, output;
            var sr;
            var osc2freq, buf, bufR;

            bufR = LocalIn.ar(2,0);
            rungler = bufR.at(0);
            buf = bufR.at(1);

            sr = SampleDur.ir;
            //sr = ControlDur.ir;
            tri1 = LFTri.ar((rungler*rungler1)+freq1);
            tri2 = LFTri.ar((rungler*rungler2)+freq2);
            osc1 = PulseDPW.ar((rungler*rungler1)+freq1);
            osc2 = PulseDPW.ar((rungler*rungler2)+freq2);

            //pwm = tri1 > tri2;
            pwm = BinaryOpUGen('>', (tri1 + tri2),(0));

            osc1 = ((buf*loop)+(osc1* (loop* -1 +1)));
            sh0 = BinaryOpUGen('>', osc1, 0.5);
            sh0 = BinaryOpUGen('==', (sh8 > sh0), (sh8 < sh0));
            sh0 = (sh0 * -1) + 1;

            sh1 = DelayN.ar(Latch.ar(sh0,osc2),0.01,sr);
            sh2 = DelayN.ar(Latch.ar(sh1,osc2),0.01,sr*2);
            sh3 = DelayN.ar(Latch.ar(sh2,osc2),0.01,sr*3);
            sh4 = DelayN.ar(Latch.ar(sh3,osc2),0.01,sr*4);
            sh5 = DelayN.ar(Latch.ar(sh4,osc2),0.01,sr*5);
            sh6 = DelayN.ar(Latch.ar(sh5,osc2),0.01,sr*6);
            sh7 = DelayN.ar(Latch.ar(sh6,osc2),0.01,sr*7);
            sh8 = DelayN.ar(Latch.ar(sh7,osc2),0.01,sr*8);

            //rungler = ((sh6/8)+(sh7/4)+(sh8/2)); //original circuit
            //rungler = ((sh5/16)+(sh6/8)+(sh7/4)+(sh8/2));

            rungler = ((sh1/2.pow(8))+(sh2/2.pow(7))+(sh3/2.pow(6))+(sh4/2.pow(5))+(sh5/2.pow(4))+(sh6/2.pow(3))+(sh7/2.pow(2))+(sh8/2.pow(1)));

            buf = rungler;
            rungler = (rungler * scale.linlin(0,1,0,127));
            rungler = rungler.midicps;

            LocalOut.ar([rungler,buf]);



            filt = Select.ar(filterType, [
                RLPF.ar(pwm,(rungler*runglerFilt)+filtFreq, q* -1 +1,gain),
                //BMoog.ar(pwm,(rungler*runglerFilt)+filtFreq, q,0,gain),
                RHPF.ar(pwm,(rungler*runglerFilt)+filtFreq, q* -1 +1,gain),
                SVF.ar(pwm,(rungler*runglerFilt)+filtFreq, q, 1,0,0,0,0,gain),
                DFM1.ar(pwm,(rungler*runglerFilt)+filtFreq, q, gain,1)
            ]);


            output = Select.ar(outSignal, [
                tri1, osc1, tri2, osc2, pwm, sh0, filt
            ]);

            output = (output * amp).tanh;
            output = [DelayL.ar(output, delaytime: pan.clip(0,1).lag(0.1)), DelayL.ar(output, delaytime: (pan.clip(-1,0) * -1).lag(0.1))];
            Out.ar(out, LeakDC.ar(output));
        }).add;

        context.server.sync;

        benjolisSynth = Synth(\benjolis, [\out, context.out_b.index]);

        this.addCommand(\setFreq1, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\freq1, val);
        });

        this.addCommand(\setFreq2, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\freq2, val);
        });

        this.addCommand(\setFiltFreq, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\filtFreq, val);
        });

        this.addCommand(\setQ, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\q, val);
        });

        this.addCommand(\setGain, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\gain, val);
        });

        this.addCommand(\setFilterType, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\filterType, val);
        });

        this.addCommand(\setRungler1, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\rungler1, val);
        });

        this.addCommand(\setRungler2, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\rungler2, val);
        });

        this.addCommand(\setRunglerFilt, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\runglerFilt, val);
        });

        this.addCommand(\setLoop, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\loop, val);
        });

        this.addCommand(\setScale, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\scale, val);
        });

        this.addCommand(\setOutSignal, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\outSignal, val);
        });

        this.addCommand(\setAmp, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\amp, val);
        });

        this.addCommand(\setPan, "f", { arg msg;
            var val = msg[1].asFloat;
            benjolisSynth.set(\pan, val * 0.001);
        });

	}

	free {
		voiceGroup.free;
		lfo.free;
		mixer.free;
    benjolisSynth.free;
	}
}
