import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;

/* A class with the main function and Processing visualizations to run the demo */

public class ClassifyVibration extends PApplet {

	FFT fft;
	AudioIn in;
	Waveform waveform;
	int bands = 512;
	int nsamples = 1024;
	float[] spectrum = new float[bands];
	float[] fftFeatures = new float[bands];
	String[] classNames = {"Nothing", "Zero", "One", "Two"};
	int classIndex = 0;
	int dataCount = 0;
	boolean if_nothing = false;
	String guessedLabel;
	String previous_guessLabel; 
	int count = 0;
	Map<String, List<DataInstance>> recordingData = new HashMap<>();
	{for (String className : classNames){
		recordingData.put(className, new ArrayList<DataInstance>());
	}}
	
	
	MLClassifier classifier;
	
	Map<String, List<DataInstance>> trainingData = new HashMap<>();
	{for (String className : classNames){
		trainingData.put(className, new ArrayList<DataInstance>());
	}}
	
	DataInstance captureInstance (String label){
		DataInstance res = new DataInstance();
		res.label = label;
		res.measurements = fftFeatures.clone();
		//println(fftFeatures.length);
		int ct = 0;
		for (int i=0; i<fftFeatures.length; i++) {
			if (fftFeatures[i] < 1E-5) {
				ct ++;
			}
			else {
				fftFeatures[i] = fftFeatures[i] * 5;
			}
		}
		if (ct >= fftFeatures.length * 0.9) {
			if_nothing = true;
		}
		else {
			if_nothing = false;
		}
		return res;
	}

	
	public static void main(String[] args) {
		PApplet.main("ClassifyVibration");
	}
	  
	public void settings() {
		size(512, 400);
	}

	public void setup() {
		
		/* list all audio devices */
		Sound.list();
		Sound s = new Sound(this);
		  
		/* select microphone device */
		s.inputDevice(2);
		    
		/* create an Input stream which is routed into the FFT analyzer */
		fft = new FFT(this, bands);
		in = new AudioIn(this, 0);
		waveform = new Waveform(this, nsamples);
		waveform.input(in);
		
		/* start the Audio Input */
		in.start();
		  
		/* patch the AudioIn */
		fft.input(in);
	}

	public void draw() {
		background(0);
		fill(0);
		stroke(255);
		
		waveform.analyze();

		beginShape();
		  
		for(int i = 0; i < nsamples; i++)
		{
			vertex(
					map(i, 0, nsamples, 0, width),
					map(waveform.data[i], -1, 1, 0, height)
					);
		}
		
		endShape();

		fft.analyze(spectrum);

		for(int i = 0; i < bands; i++){

			/* the result of the FFT is normalized */
			/* draw the line for frequency band i scaling it up by 40 to get more amplitude */
			line( i, height, i, height - spectrum[i]*height*40);
			fftFeatures[i] = spectrum[i];
		} 

		fill(255);
		textSize(30);
		if(classifier != null) {
			previous_guessLabel = guessedLabel;
			guessedLabel = classifier.classify(captureInstance(null));
			
			// Yang: add code to stabilize your classification results
			
			text("classified as: " + guessedLabel, 20, 30);

			if(previous_guessLabel != guessedLabel)
			{
				println(guessedLabel + ' ' + count + '\n');
				count = 0;
			}
			count = count+1;
			
			
		}else {
			text(classNames[classIndex], 20, 30);
			dataCount = trainingData.get(classNames[classIndex]).size();
			text("Data collected: " + dataCount, 20, 60);
		}
	}
	
	public void keyPressed() {
		

		if (key == CODED && keyCode == DOWN) {
			classIndex = (classIndex + 1) % classNames.length;
		}
		
		else if (key == 't') {
			if(classifier == null) {
				println("Start training ...");
				classifier = new MLClassifier();
				classifier.train(trainingData);
				// println(classifier);
			}else {
				classifier = null;
			}
		}
		
		
		else if (key == 's') {
			// Yang: add code to save your trained model for later use
			// println(classifier);
			classifier.SaveModel();
			
		}
		
		else if (key == 'l') {
			// Yang: add code to load your previously trained model
			classifier.LoadModel();
			println("Load");
		}
		
		else if (key == 'z') {
			if (classNames[classIndex] == "Nothing") {
				trainingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
			}
			captureInstance(classNames[classIndex]);
			if (!if_nothing) {
				trainingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
				//recordingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
				println("recording");
			}
			println(if_nothing);
		}
			
		//else {
		//	trainingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
		//}
	}

	private Object key(char c) {
		// TODO Auto-generated method stub
		return null;
	}

}
