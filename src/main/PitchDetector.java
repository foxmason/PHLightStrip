package main;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class PitchDetector implements PitchDetectionHandler {

	private AudioDispatcher _dispatcher;
	private Mixer _currentMixer;
	private PitchEstimationAlgorithm _algo;

	public PitchDetector(PhilipsHueManager manager) {
		_algo = PitchEstimationAlgorithm.YIN;
		for (Mixer.Info info : Shared.getMixerInfo(false, true)) {
			System.out.println(AudioSystem.getMixer(info) + " -- " + info.getName());
		}

		_currentMixer = AudioSystem.getMixer(Shared.getMixerInfo(false, true).get(2));
		setNewMixer(_currentMixer);
	}

	private void setNewMixer(Mixer mixer) {
		try {
			if (_dispatcher != null) {
				_dispatcher.stop();
			}
			_currentMixer = mixer;

			float sampleRate = 44100;
			int bufferSize = 1024;
			int overlap = 0;

			System.out.println("Started listening with " + mixer.getMixerInfo().getName());

			final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
			final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
			TargetDataLine line;
			line = (TargetDataLine) mixer.getLine(dataLineInfo);
			final int numberOfSamples = bufferSize;
			line.open(format, numberOfSamples);
			line.start();
			final AudioInputStream stream = new AudioInputStream(line);
			JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
			_dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
			_dispatcher.addAudioProcessor(new PitchProcessor(_algo, sampleRate, bufferSize, this));

			new Thread(_dispatcher, "Audio dispatching").start();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		if (pitchDetectionResult.getProbability() > .075f && pitchDetectionResult.getPitch() > 0.0f) {
			System.out.println("I heard audio: " + pitchDetectionResult.getPitch() + " -- "
					+ pitchDetectionResult.getProbability() + "%");
			//_manager.sendPitch(pitchDetectionResult.getPitch());
		}
	}
}
