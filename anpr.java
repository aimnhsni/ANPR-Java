import java.util.ArrayList;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.highgui.Highgui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.awt.image.BufferedImage;


	 

class plate_system{
    public void run(String[] args) {
        // Check number of arguments
    	
 
        // Load the image
    	
    	 System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
         String file ="test_001.jpg";
         Mat src = Highgui.imread(file, 
    	         Highgui.CV_LOAD_IMAGE_GRAYSCALE);
    	
       
     
         
        // Check if image is loaded fine
        if( src.empty() ) {
            System.out.println("Error opening image: " + args[0]);
            System.exit(-1);
        }
        
        
        // Transform source image to gray if it is not already
        
        Mat gray = new Mat();
        
        if (src.channels() == 3)
        {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        }
        else
        {
            gray = src;
        }

        // Show gray image
        showWaitDestroy("gray conversion" , gray);
        
        
        //median filter
        
        Mat median = new Mat();
        Imgproc.medianBlur(gray, median, 3);
        
        showWaitDestroy("median" , median);

        
        
        
    // Canny edge detection 
        
        Mat canny = new Mat();
        Imgproc.Canny(median, canny, 255/3, 255, 3, true);
        
        showWaitDestroy("canny operator" ,canny);
  
     
      // morphological operators 
        // dilate with large element, erode with small ones
           Mat imgDilate = new Mat();
           //Mat imgErode = new Mat();
           
   	      Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size((2*2)+1, (2*2)+1));
   	      //Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
   	
   	      //Imgproc.erode(imgThreshold, imgErode, erodeElement);
   	
   	      Imgproc.dilate(canny, imgDilate, dilateElement);
   	      
   	      //HighGui.imshow("Erode", imgErode);
   	   showWaitDestroy("Dilation" ,imgDilate);
        
        
        //Find contours 
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
	
        Mat contourImg = new Mat(canny.size(), canny.type());
        
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(contourImg, contours, i, new Scalar(255, 255, 255), -1);
            
        }

        //Imgcodecs.imwrite("src/picture/contours_in Original.png", contourOri);
        
        showWaitDestroy("Contours", contourImg);
       
        
        //Sort contour
       List<MatOfPoint> newContour = new ArrayList<MatOfPoint>();
       //List<Double> area_cont = new ArrayList<Double>();
       Mat new_img = src;
       double maxArea = 0;
       //int maxAreaIdx = 0;
       
       for(int i=0; i<contours.size(); i++) {
    	   double area = Imgproc.contourArea(contours.get(i));
    	   
    	   if(maxArea<area) {
    		   maxArea = area;
    		   newContour.add(contours.get(i));
    	   }
    	  
       }
         
  
       
       Imgproc.drawContours(new_img, newContour, -1, new Scalar(0, 0, 255), 2);
       
       showWaitDestroy("Top Contour", new_img);
       
              
         
    //Find approximate number plate 
        
      
        List<MatOfPoint> rectangle = new ArrayList<MatOfPoint>();
        Mat topcontour = gray;
        MatOfPoint2f approx = new MatOfPoint2f();
        
        for (MatOfPoint contour : newContour) {
        	
            MatOfPoint2f dst = new MatOfPoint2f();
            contour.convertTo(dst, CvType.CV_32F);
            double perimeter = Imgproc.arcLength(dst, true);
            double approximationAccuracy = 0.02 * perimeter;
            

            Imgproc.approxPolyDP(dst, approx, approximationAccuracy, true);
            if (approx.total() == 4) {
                rectangle.add(contour);
                
            }
            
            
        }

        for (int i=0;i<rectangle.size();i++) {
        	Imgproc.drawContours(topcontour, rectangle, i, new Scalar(0, 0, 255), 2);
        }
        
        //System.out.println(rectangle.size());
        
        showWaitDestroy("Approximate", topcontour);
       
        
       
        // Crop contours 
        
        Rect plate_rect = new Rect();
        Mat image = topcontour;
        Mat ROI = new Mat();
        double minAR = 2;
        double maxAR = 5;
       
        for (int i=0; i<rectangle.size(); i++) {
        	double cont_area = Imgproc.contourArea(rectangle.get(i));
        	plate_rect = Imgproc.boundingRect(rectangle.get(i));
        	
        	double rect_ratio = plate_rect.width / plate_rect.height; 
        	
            if(rect_ratio>=minAR && rect_ratio<=maxAR  ) {
            	
        	
        		ROI = image.submat(plate_rect.y, plate_rect.y + plate_rect.height, plate_rect.x, plate_rect.x + plate_rect.width);
        	
        	}
            
        }
        
        if(ROI.empty()  ) {
        	System.out.println("Sorry, number plate cannot be detected!");
        }
        
        else { //output
        	
        	
        	showWaitDestroy("Number Plate", ROI);
        	System.exit(0);
        }
        
        
	}
   
    
    private void showWaitDestroy(String winname, Mat img) {
        HighGui.imshow(winname, img);
        HighGui.moveWindow(winname, 500, 0);
        HighGui.waitKey(0);
        HighGui.destroyWindow(winname);
    }
}
public class anpr {
    public static void main(String[] args) {
        // Load the native library.
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        new plate_system().run(args);
        
        
    }
}
