package main;

import com.philips.lighting.hue.sdk.PHHueSDK;

public class PhilipsHueManager {

	private Controller _controller;

	// private ArrayList<Double> _cache = new ArrayList<Double>();
	//
	// public PhilipsHueManager() {
	// PHHueSDK phHueSDK = PHHueSDK.create();
	// HueProperties.loadProperties();
	//
	// _cache.add(500.0);
	// _cache.add(500.0);
	// _cache.add(500.0);
	// _cache.add(500.0);
	// _cache.add(500.0);
	//
	// _controller = new Controller();
	// phHueSDK.getNotificationManager().registerSDKListener(_controller.getListener());
	//
	// _controller.findBridges();
	// }
	//
	// public static void main(String[] args) {
	// PhilipsHueManager manager = new PhilipsHueManager();
	// new PitchDetector(manager);
	// }
	//
	// public void sendPitch(float pitch) {
	// if (_cache.size() >= 2) {
	// _cache.remove(0);
	// }
	//
	// _cache.add((double) pitch);
	//
	// double sum = 0.0;
	// for (double d : _cache) {
	// sum += d;
	// }
	// _controller.setLightsBasedOnPitch(sum / 2.0);
	// }

	public PhilipsHueManager() {
		PHHueSDK phHueSDK = PHHueSDK.create();
		HueProperties.loadProperties();

		_controller = new Controller();
		phHueSDK.getNotificationManager().registerSDKListener(_controller.getListener());

		_controller.findBridges();
	}

	public static void main(String[] args) {
		new PhilipsHueManager();
	}
}
