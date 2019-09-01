package main;

import com.philips.lighting.hue.sdk.PHHueSDK;

public class PhilipsHueManager {

	private Controller _controller;

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
