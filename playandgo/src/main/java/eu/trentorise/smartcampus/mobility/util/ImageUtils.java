package eu.trentorise.smartcampus.mobility.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;


public class ImageUtils {

//		public static byte[] compressImage(InputStream is, String mediaType, int dimension) throws IOException {
//			BufferedImage bs = ImageIO.read(is);
//			bs = rescale(bs, dimension, dimension);
//			return compress(bs, mediaType);
//		}
		
		public static byte[] compressImage(BufferedImage bs, String mediaType, int dimension) throws IOException {
			BufferedImage bsr = rescale(bs, dimension, dimension);
			return compress(bsr, mediaType);
		}		
	
	   private static BufferedImage rescale(BufferedImage bi, int w, int h) throws IOException {
	        int ow = bi.getWidth();
	        int oh = bi.getHeight();
	        int type = bi.getType() == 0? BufferedImage.TYPE_INT_ARGB : bi.getType();

	        boolean horiz = false;
	        if (ow > oh) {
	        	horiz = true;
	        }
	        double r = (double)ow / oh;
	        
	        int nw = w;
	        int nh = h;
	        
	        if (horiz) { // ! for crop
	        	nh = (int)(h / r);
	        } else {
	        	nw = (int)(w * r);
	        }
	        
	        int dw = 0;
	        int dh = 0;
	        
	        // reenable for crop
//	        if (nw > w) {
//	        	dw = (nw - w) / 2;
//	        }
//	        if (nh > h) {
//	        	dh = (nh - h) / 2;
//	        }	        
	        
//	        System.err.println(ow + "/" + oh + " -> " + nw + "/" + nh);
//	        System.err.println(dw + "/" + dh);
	        
	        //rescale 50%
	        BufferedImage resizedImage = new BufferedImage(nw - 2 * dw, nh - 2 * dh, type);
	        Graphics2D g = resizedImage.createGraphics();
	        g.drawImage(bi, 0 - dw, 0 - dh, nw - dw, nh - dh, null);
	        g.dispose();
	        g.setComposite(AlphaComposite.Src);
	        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
	        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
	        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_QUALITY);

	        return resizedImage;
	    }

	    private static byte[] compress(BufferedImage bi, String mediaType)
	            throws FileNotFoundException, IOException {
	        String mtype[] = mediaType.split("/");
	        String type = mtype[1];	    	
	    	
	        Iterator<ImageWriter> i = ImageIO.getImageWritersByFormatName(type);
	        
	        ImageWriter imageWriter = i.next();

	        ImageWriteParam param = imageWriter.getDefaultWriteParam();
	        
	        if (!"png".equals(type)) {
	        	param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	        }
	        if ("jpg".equals(type) || "jpeg".equals(type)) {
	        	param.setCompressionQuality(0.5f);
	        } else if ("gif".equals(type)) {
	        	param.setCompressionType("LZW");
	        }
	        
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bos);
	        imageWriter.setOutput(out);
	        imageWriter.write(null, new IIOImage(bi, null, null), param);
	        imageWriter.dispose();
	        out.close();
	        
	        return bos.toByteArray();
	    }
}