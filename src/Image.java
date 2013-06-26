import java.util.ArrayList;


public class Image {
	String path;
	boolean hasFaceData = false;
	boolean hasChild = false;
	long index;
	int h;
	int w;
	ArrayList<Face> faces;
	
	public Image(String path, long index, int w, int h) {
		this.path=path;
		this.index=index;
		this.h=h;
		this.w=w;
		faces = new ArrayList<Face>();
	}
	
	public Face addFace(String facerect, String person){
		int fx1 = (int) (w*(str4HextoDec(facerect.substring(0, 4))/65535.0F));
		int fy1 = (int) (h*(str4HextoDec(facerect.substring(4, 8))/65535.0F));
		int fx2 = (int) (w*(str4HextoDec(facerect.substring(8, 12))/65535.0F));
		int fy2 = (int) (h*(str4HextoDec(facerect.substring(12, 16))/65535.0F));
		Face f = new Face(fx1, fy1, fx2-fx1, fy2-fy1, person, this);
		faces.add(f);
		return f;
	}
	
	public long str4HextoDec(String str){
		long ret = Long.parseLong(str,16);
	        return ret;
	}
	
	public String toString(){
		String s = "\n"+path+"\nw:"+w+", h:"+h;
		if(faces.size()!=0){
			s+="\nFaces:";
			for (Face f : faces) {
				s+="\n"+f.toString();
			}
		}
		return s;
	}
}
