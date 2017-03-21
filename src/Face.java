
public class Face {
	int x;
	int y;
	int w;
	int h;
	float region_x;
	float region_y;
	float region_w;
	float region_h;
	
	Image img;
	String name;
	
	public Face(int x, int y, int w, int h, String name, Image i) {
		this.x=x;
		this.y=y;
		this.w=w;
		this.h=h;
		this.name=name;
		this.img=i;
	}

	public Face(int x, int y, int w, int h, float region_x, float region_y, float region_w, float region_h, String name, Image i) {
		this.x=x;
		this.y=y;
		this.w=w;
		this.h=h;
		this.name=name;
		this.img=i;
		this.region_x=region_x;
		this.region_y=region_y;
		this.region_w=region_w;
		this.region_h=region_h;
	}

	public String toString(){
		String s= name+"\nx:"+x+", y:"+y+", w:"+w+", h:"+h;
		s+= "\nFrom bottom left corner:\nx:"+(x-w/2)+", y:"+(y-h/2)+", w:"+w+", h:"+h;
		return s;
	}
}
