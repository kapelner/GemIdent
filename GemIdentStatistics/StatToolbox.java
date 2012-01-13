package GemIdentStatistics;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.special.Gamma;

/**
 * This is a class where we're going to put all sorts of useful functions
 * as a utility-style class
 */
public class StatToolbox {

	/**
	 * Wikipedia parameterization
	 */
	public static final double ILLEGAL_FLAG = -999999999;

	public static double sample_from_inv_gamma(double k, double theta){
		return 1 / sample_from_gamma(k, theta);
	}
	
	public static double sample_from_gamma(double k, double theta){
		GammaDistributionImpl gamma_dist = new GammaDistributionImpl(k, theta);
		try {
			return gamma_dist.inverseCumulativeProbability(Math.random());
		} catch (MathException e) {
			e.printStackTrace();
		}
		return ILLEGAL_FLAG;
	}
	
	public static double inv_norm_dist(double p){
		//System.out.println("inv_norm_dist p=" + p);
		try {
			return new NormalDistributionImpl().inverseCumulativeProbability(p);
		} catch (MathException e) {
			e.printStackTrace();
		}
		return ILLEGAL_FLAG;
	}
	
	public static double sample_from_norm_dist(double mu, double sigma){
//		System.out.println("sample_from_norm_dist mu=" + mu + " sigsq=" + sigma);
		try {
			return new NormalDistributionImpl(mu, sigma).inverseCumulativeProbability(Math.random());
		} catch (MathException e) {
			e.printStackTrace();
		}
		return ILLEGAL_FLAG;		
	}
	
	public static final double sample_average(double[] y){
		double y_bar = 0;
		for (int i = 0; i < y.length; i++){
			y_bar += y[i];
		}
		return y_bar / (double)y.length;
	}
	
	public static final double sample_average(int[] y){
		double y_bar = 0;
		for (int i = 0; i < y.length; i++){
			y_bar += y[i];
		}
		return y_bar / (double)y.length;
	}	
	
	public static final double sample_standard_deviation(int[] y){
		double y_bar = sample_average(y);
		double sum_sqd_deviations = 0;
		for (int i = 0; i < y.length; i++){
			sum_sqd_deviations += Math.pow(y[i] - y_bar, 2);
		}
		return Math.sqrt(sum_sqd_deviations / ((double)y.length - 1));		
	}
	
	public static final double sample_standard_deviation(double[] y){
		return Math.sqrt(sample_variance(y));
	}	
	
	public static final double sample_variance(double[] y){
		return sample_sum_sq_err(y) / ((double)y.length - 1);		
	}		
	
	public static final double sample_sum_sq_err(double[] y){
		double y_bar = sample_average(y);
		double sum_sqd_deviations = 0;
		for (int i = 0; i < y.length; i++){
			sum_sqd_deviations += Math.pow(y[i] - y_bar, 2);
		}
		return sum_sqd_deviations;
	}
	
	public static final double cumul_dens_function_inv_gamma(double alpha, double beta, double lower, double upper){
		return cumul_dens_function_inv_gamma(alpha, beta, upper) - cumul_dens_function_inv_gamma(alpha, beta, lower);
	}
	
	public static final double cumul_dens_function_inv_gamma(double alpha, double beta, double x){
		try {
			return Gamma.regularizedGammaQ(alpha, beta / x);
		} catch (MathException e) {
			e.printStackTrace();
		}
		return ILLEGAL_FLAG;
	}	
	
	public static final double inverse_cumul_dens_function_inv_gamma(double nu, double lambda, double p){
//		try {
			return nu * lambda / (nu - 2);
//		} catch (MathException e) {
//			e.printStackTrace();
//		}
//		return ILLEGAL_FLAG;
	}	

	public static double sample_minimum(int[] y) {
		int min = Integer.MAX_VALUE;
		for (int y_i : y){
			if (y_i < min){
				min = y_i;
			}
		}
		return min;
	}

	public static double sample_maximum(int[] y) {
		int max = Integer.MIN_VALUE;
		for (int y_i : y){
			if (y_i > max){
				max = y_i;
			}
		}
		return max;		
	}

	public static double sample_minimum(double[] y){
		double min = Double.MAX_VALUE;
		for (double y_i : y){
			if (y_i < min){
				min = y_i;
			}
		}
		return min;		
	}
	
	public static double sample_maximum(double[] y){
		double max = Double.MIN_VALUE;
		for (double y_i : y){
			if (y_i > max){
				max = y_i;
			}
		}
		return max;			
	}
	/**
	 * Given an array, return the index that houses the maximum value
	 * 
	 * @param arr	the array to be investigated
	 * @return		the index of the greatest value in the array
	 */
	public static int FindMaxIndex(int[] arr){
		int index=0;
		int max=Integer.MIN_VALUE;
		for (int i=0;i<arr.length;i++){
			if (arr[i] > max){
				max=arr[i];
				index=i;
			}				
		}
		return index;
	}	
}
