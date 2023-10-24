package bia.quality.noise;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageStatistics;

@Plugin(type = Command.class, menuPath="Plugins>BIA Quality Tools>Noise Simulator")
public class NoiseSimulator extends DynamicCommand {

	ImagePlus resultImage = null;
	ImagePlus noisyImage = null;
	
	@Parameter
	ImagePlus imp;
	
	@Parameter (label = "psf Sigma (def=2)", callback = "addNoise", min = "0")
	Integer psfSigma = 2;
	
	@Parameter (label = "Background Photons (def=10)", callback = "addNoise", min = "0")
	Integer backgroundPhotons = 10;
	
	@Parameter (label = "Exposure Time (def=10)", callback = "addNoise", min = "0")
	Integer exposureTime = 10;
	
	@Parameter (label = "Read Std Dev(def=5)", callback = "addNoise", min = "0")
	Integer readStdDev = 5;
	
	@Parameter (label = "Detector Gain (def=1)", callback = "addNoise", min = "0")
	Integer detectorGain = 1;
	
	@Parameter (label = "Detector Offset(def=100)", callback = "addNoise", min = "0")
	Integer detectorOffset = 100;
	
	@Parameter (label = "nBits (def=8)", callback = "addNoise", min = "0")
	Integer nBits = 8;
	
	@Parameter (label = "Max Photon Emission (def=10)", callback = "addNoise", min = "0")
	Integer maxPhotonEmission = 10;
	
	@Parameter (label = "Do Binning", callback = "addNoise")
	Boolean doBinning = false;
	
	
	public void addNoise() {
		noisyImage = new ImagePlus(imp.getTitle() + "_noise", imp.duplicate().getProcessor().convertToFloatProcessor());
		//ImageProcessor resultProcessor = resultImp.getProcessor();
		
		// Normalize the image to the range 0-1
		ImageStatistics stats = imp.getStatistics();
		double divisor = stats.max - stats.min;
		IJ.run(noisyImage, "Subtract...", "value=" + stats.min);
		IJ.run(noisyImage, "Divide...", "value=" + divisor);
		
		// Define the photon emission at the brightest point
		IJ.run(noisyImage, "Multiply...", "value=" + maxPhotonEmission);

		// Simulate PSF blurring
		IJ.run(noisyImage, "Gaussian Blur...", "sigma=" + psfSigma);

		// Add background photons
		IJ.run(noisyImage, "Add...", "value=" + backgroundPhotons);

		// Multiply by the exposure time
		IJ.run(noisyImage, "Multiply...", "value=" + exposureTime);

		// Simulate photon noise
		//run("RandomJ Poisson", "mean=1.0 insertion=modulatory");

		// Simulate the detector gain
		// (note this should really add Poisson noise too!)
		IJ.run(noisyImage, "Multiply...", "value=" + detectorGain);

		// Simulate binning (optional)
		if (doBinning) {
			IJ.run(noisyImage, "Bin...", "x=2 y=2 bin=Sum");
		}
		
		// Simulate the detector offset
		IJ.run(noisyImage, "Add...", "value="+detectorOffset);

		// Simulate read noise
		IJ.run(noisyImage, "Add Specified Noise...", "standard="+readStdDev);

		// Clip any negative values
		IJ.run(noisyImage, "Min...", "value=0");

		// Clip the maximum values based on the bit-depth
		double maxVal = Math.pow(2, nBits) - 1;
		IJ.run(noisyImage, "Max...", "value=" + maxVal);
			
		resultImage = WindowManager.getImage(imp.getTitle() + "_noise");
		System.out.println(resultImage);
		
		if (resultImage == null) {
			resultImage = new ImagePlus(imp.getTitle() + "_noise", noisyImage.getProcessor());
			resultImage.show();
		} else {
			resultImage.setProcessor(noisyImage.getProcessor());
			resultImage.updateAndDraw();
		}
		resultImage.getWindow().setLocation(imp.getWindow().getLocation().x + imp.getWindow().getWidth(), imp.getWindow().getLocation().y);
		resultImage.getWindow().setSize(imp.getWindow().getWidth(), imp.getWindow().getHeight());
		resultImage.getCanvas().setScaleToFit(true);
	}	
}
