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
	String[] classNames = {"Nothing", "Zero", "One"};
	int classIndex = 0;
	int dataCount = 0;
	boolean if_nothing = false;
	String guessedLabel;
	String previous_guessLabel; 
	String outputLabel;
	String firstLabel = "Nothing";
	int count = 0;
	int Guess_ct = 0;
	int Nothing_ct = 0;
	int Zero_ct = 0;
	int One_ct = 0;
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
		/*  
		for(int i = 0; i < nsamples; i++)
		{
			vertex(
					map(i, 0, nsamples, 0, width),
					map(waveform.data[i], -1, 1, 0, height)
					);
		}
		*/
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
			// Yang: add code to stabilize your classification results
			if (Guess_ct < 25) {
				guessedLabel = classifier.classify(captureInstance(null));
				if (guessedLabel == "Nothing") {
					Nothing_ct++;
				}else if (guessedLabel == "Zero"){
					if (firstLabel == "Nothing") { firstLabel = "Zero";}
					Zero_ct++;
				}else if (guessedLabel == "One") {
					if (firstLabel == "Nothing") { firstLabel = "One";}
					One_ct++;
				}
				Guess_ct ++;
			}else {
				if (Nothing_ct >= 25) {
					outputLabel = "Nothing";
				}else {
					if (Zero_ct > One_ct) {
						outputLabel = "Zero";
					}else if (One_ct > Zero_ct) {
						outputLabel = "One";
					}else if (Zero_ct == One_ct) {
						outputLabel = firstLabel;
						println("FirstLabel used!!!");
					}
				}
				println("Output: " + outputLabel);
				println("Guess_ct: " + Guess_ct);
				println("Nothing_ct: " + Nothing_ct);
				println("Zero_ct: " + Zero_ct);
				println("One_ct: " + One_ct);
				Guess_ct = 0;
				Nothing_ct = 0;
				Zero_ct = 0;
				One_ct = 0;
				firstLabel = "Nothing";
			}
			
			text("classified as: " + outputLabel, 20, 30);
			if (outputLabel == "Zero") {
				line(100, 300, 350, 300);
				fill(0);
				rect(350, 100, 20, 200);
				rect(370, 280, 100, 20);
			}
			if (outputLabel == "One") {
				line(100, 300, 350, 300);
				fill(0);
				rect(350, 100, 20, 200);
				rect(370, 280, 100, 20);
				fill(165, 42, 42);
				rect(305, 140, 40, 160);
				rect(260, 140, 40, 160);
			}
					
			
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
