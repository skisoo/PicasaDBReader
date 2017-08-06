package be.thebaseline.kmoens.PicasaDBReader;


import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Date;


public class ReadFunctions {
	public final static String dumpStringField(DataInputStream din)
	        throws IOException
	    {
	            return getString(din);
	    }

	    public final static String dumpByteField(DataInputStream din)
	        throws IOException
	    {
	            Integer i = new Integer( din.readUnsignedByte());
	            return i.toString();
	    }

	    public final static String dump2byteField(DataInputStream din)
	        throws IOException
	    {
	            Integer i = new Integer(  readUnsignedShort(din));
	            return i.toString();
	    }

	    public final static String dump4byteField(DataInputStream din)
	        throws IOException
	    {
	            Long i = new Long(  readUnsignedInt(din));
	            return i.toString();
	    }
	 
	    public final static String dump8byteField(DataInputStream din)
	        throws IOException
	    {
	        StringBuilder s = new StringBuilder();
	        int[] bytes = new int[8];
	            for (int i=0; i<8; i++) {
	                bytes[i] = din.readUnsignedByte();
	            }
	            for (int i=7; i>=0; i--) {
	                String x = Integer.toString(bytes[i],16);
	                if (x.length() == 1) {
	                    s.append("0");
	                }
	                s.append(x);
	            }
	        return s.toString();
	    }

	    public final static String dumpDateField(DataInputStream din)
	        throws IOException
	    {
	        StringBuilder s = new StringBuilder();
	        int[] bytes = new int[8];
	            long ld = 0;
	            for (int i=0; i<8; i++) {
	                bytes[i] = din.readUnsignedByte();
	                long tmp = bytes[i];
	                tmp <<= (8*i);
	                ld += tmp;
	            }
	            for (int i=7; i>=0; i--) {
	                String x = Integer.toString(bytes[i],16);
	                if (x.length() == 1) {
	                }
	            }
	            double d = Double.longBitsToDouble(ld);
	            // days past unix epoch.
	            d -= 25569d;
	            long ut = Math.round(d*86400l*1000l);
	            s.append(new Date(ut));
	        
	        return s.toString();
	    }

	    public final static String getString(DataInputStream din)
	        throws IOException
	    {
	        StringBuffer sb = new StringBuffer();
	        int c;
	        while((c = din.read()) != 0) {
	            sb.append((char)c);
	        }
	        byte[] b = sb.toString().getBytes("ISO-8859-1");
	        return new String(b, "UTF-8");
	    }

	    public final static int readUnsignedShort(DataInputStream din)
	        throws IOException
	    {
	        int ch1 = din.read();
	        int ch2 = din.read();
	        if ((ch1 | ch2) < 0)
	            throw new EOFException();
	        return ((ch2<<8) + ch1<<0);
	    }

	    public final static long readUnsignedInt(DataInputStream din)
	        throws IOException
	    {
	        int ch1 = din.read();
	        int ch2 = din.read();
	        int ch3 = din.read();
	        int ch4 = din.read();
	        if ((ch1 | ch2 | ch3 | ch4) < 0)
	            throw new EOFException();

	        long ret = 
	            (((long)ch4)<<24) +
	            (((long)ch3)<<16) +
	            (((long)ch2)<<8) +
	            (((long)ch1)<<0);
	        return ret;
	    }
	    
	    public final static void read26(DataInputStream din)
	            throws IOException
	        {
	            for(int i=0; i<26; i++){
	                din.read();
	            }
	        }
}
