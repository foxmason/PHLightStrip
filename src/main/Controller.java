package main;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPort;
import com.pi4j.io.serial.StopBits;

public class Controller {

	private PHHueSDK phHueSDK;
	private static final int MAX_HUE = 65535;

	public Controller() {
		this.phHueSDK = PHHueSDK.getInstance();
	}

	public void findBridges() {
		phHueSDK = PHHueSDK.getInstance();
		PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK
				.getSDKService(PHHueSDK.SEARCH_BRIDGE);
		sm.search(true, true);
	}

	/**
	 * Connect to the last known access point. This method is triggered by the
	 * Connect to Bridge button but it can equally be used to automatically
	 * connect to a bridge.
	 * 
	 */
	public boolean connectToLastKnownAccessPoint() {
		String username = HueProperties.getUsername();
		String lastIpAddress = HueProperties.getLastConnectedIP();

		if (username == null || lastIpAddress == null) {
			System.out
					.println("Missing Last Username or Last IP.  Last known connection not found.");
			return false;
		}

		PHAccessPoint accessPoint = new PHAccessPoint();
		accessPoint.setIpAddress(lastIpAddress);
		accessPoint.setUsername(username);
		phHueSDK.connect(accessPoint);
		return true;
	}

	private PHSDKListener listener = new PHSDKListener() {
		public void onAccessPointsFound(List<PHAccessPoint> accessPointsList) {
			System.out.println("Access points found...");
			phHueSDK.connect(accessPointsList.get(0));
			System.out.println("Connecting to "
					+ accessPointsList.get(0).getBridgeId());
		}

		public void onAuthenticationRequired(PHAccessPoint accessPoint) {
			phHueSDK.startPushlinkAuthentication(accessPoint);
			System.out.println("Authentication required...");
		}

		public void onBridgeConnected(PHBridge bridge, String username) {
			phHueSDK.setSelectedBridge(bridge);
			phHueSDK.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);
			System.out.println("Bridge Connected...");
			String lastIpAddress = bridge.getResourceCache()
					.getBridgeConfiguration().getIpAddress();
			HueProperties.storeUsername(username);
			HueProperties.storeLastIPAddress(lastIpAddress);
			HueProperties.saveProperties();

			syncAndSendData();
		}

		public void onCacheUpdated(List<Integer> arg0, PHBridge arg1) {
		}

		public void onConnectionLost(PHAccessPoint arg0) {
		}

		public void onConnectionResumed(PHBridge arg0) {
		}

		public void onError(int code, final String message) {
			System.out.println(message);
		}

		public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
			for (PHHueParsingError parsingError : parsingErrorsList) {
				System.out.println("ParsingError : "
						+ parsingError.getMessage());
			}
		}
	};

	public PHSDKListener getListener() {
		return listener;
	}

	public void setListener(PHSDKListener listener) {
		this.listener = listener;
	}

	//

	//

	//
	private Serial _serial;

	public void initiatePorts() {
		_serial = SerialFactory.createInstance();
		try {
			SerialConfig config = new SerialConfig();
			config.device("/dev/ttyUSB0").baud(Baud._9600)
					.dataBits(DataBits._8).parity(Parity.NONE)
					.stopBits(StopBits._1).flowControl(FlowControl.NONE);

			_serial.open(config);
		} catch (Exception ex) {
			System.out.println("SERIAL SETUP FAILED : " + ex.getMessage());
			return;
		}
	}

	public void syncAndSendData() {
		PHBridge bridge = phHueSDK.getSelectedBridge();
		PHBridgeResourcesCache cache = bridge.getResourceCache();

		List<PHLight> allLights = cache.getAllLights();

		int target = 0;

		for (int i = 0; i < allLights.size(); i++) {
			if (allLights.get(i).getName().equals("Living Room Mason")) {
				target = i;
			}
		}

		PHLightState phls = null;
		int rgb = 0;

		while (true) {
			cache = bridge.getResourceCache();
			allLights = cache.getAllLights();

			phls = allLights.get(target).getLastKnownLightState();
			rgb = Color.HSBtoRGB((float) (phls.getHue() / 65535.0), (float) (phls.getSaturation() / 255.0), (float) (phls.getBrightness() / 255.0));
			Color color = new Color(rgb);
			float[] comp = color.getRGBColorComponents(null);			
			int r = (int) (comp[0] * 255);
			int g = (int) (comp[1] * 255);
			int b = (int) (comp[2] * 255);

			if (!phls.isOn()) {
				r = 0;
				g = 0;
				b = 0;
			}			
			
			System.out.println(allLights.get(target).getName() + " => " +  r + " : " + g + " : " + b);
			exportRGB(r, g, b);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void exportRGB(int r, int g, int b) {
		try {
			_serial.writeln((r + 100) + ":" + (g + 100) + ":" + (b + 100));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startSyncThread() {
		Thread thread = new Thread() {
			public void run() {
				PHBridge bridge = phHueSDK.getSelectedBridge();
				PHBridgeResourcesCache cache = bridge.getResourceCache();

				PHLight target = null;
				List<PHLight> allLights = cache.getAllLights();
				for (PHLight light : allLights) {
					if (light.getIdentifier() == "LED_Strip") {
						target = light;
					}
				}

				while (target != null) {
					System.out.println("{H: "
							+ target.getLastKnownLightState().getHue() + " S: "
							+ target.getLastKnownLightState().getSaturation()
							+ " V: "
							+ target.getLastKnownLightState().getBrightness()
							+ "}");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}

	// /

	public void randomLights() {
		PHBridge bridge = phHueSDK.getSelectedBridge();
		PHBridgeResourcesCache cache = bridge.getResourceCache();

		List<PHLight> allLights = cache.getAllLights();

		Random rand = new Random();

		for (PHLight light : allLights) {
			PHLightState lightState = new PHLightState();
			lightState.setHue(rand.nextInt(MAX_HUE));
			bridge.updateLightState(light, lightState); // If no bridge response
														// is required then use
														// this simpler form.
		}
	}

	public void constantRandomizing() {
		while (true) {
			PHBridge bridge = phHueSDK.getSelectedBridge();
			PHBridgeResourcesCache cache = bridge.getResourceCache();

			List<PHLight> allLights = cache.getAllLights();
			Random rand = new Random();

			for (PHLight light : allLights) {
				PHLightState lightState = new PHLightState();
				lightState.setHue(rand.nextInt(MAX_HUE));
				lightState.setTransitionTime(0);
				bridge.updateLightState(light, lightState); // If no bridge
															// response is
															// required then use
															// this simpler
															// form.
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// public void setLightsBasedOnPitch(double pitch) {
	// if (_connected) {
	// double ratio = (pitch - 75.0) / 500.0;
	// if (ratio > 1) {
	// ratio = 1;
	// }
	// if (ratio < 0) {
	// ratio = 0;
	// }
	//
	// PHBridge bridge = phHueSDK.getSelectedBridge();
	// PHBridgeResourcesCache cache = bridge.getResourceCache();
	//
	// List<PHLight> allLights = cache.getAllLights();
	// Random rand = new Random();
	//
	// for (PHLight light : allLights) {
	// PHLightState lightState = new PHLightState();
	// lightState.setColorMode(PHLightColorMode.COLORMODE_XY);
	// // lightState.setEffectMode(PHLightEffectMode.EFFECT_COLORLOOP);
	// lightState.setX((float) (ratio * .8f));
	// lightState.setY(.25f);
	// // lightState.setHue((int) (MAX_HUE * ratio));
	// lightState.setBrightness(100);
	// lightState.setTransitionTime(20);
	// bridge.updateLightState(light, lightState);
	// }
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// }
	// }

	// public void setLights(boolean on) {
	// System.out.println("HIt");
	// PHBridge bridge = phHueSDK.getSelectedBridge();
	// PHBridgeResourcesCache cache = bridge.getResourceCache();
	//
	// List<PHLight> allLights = cache.getAllLights();
	// Random rand = new Random();
	//
	// for (PHLight light : allLights) {
	// if (on) {
	// PHLightState lightState = new PHLightState();
	// lightState.setHue(rand.nextInt(MAX_HUE));
	// } else {
	// PHLightState lightState = new PHLightState();
	// bridge.updateLightState(light, lightState);
	// }
	// }
	// }
}
