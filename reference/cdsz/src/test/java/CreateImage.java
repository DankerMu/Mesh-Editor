import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class CreateImage {
	public static void main(String[] args) {
		String tuli = "E:/fl/参考资料/图例1.png";
		try {
			BufferedImage image1 = ImageIO.read(new File(tuli));
			ImageIO.write(image1, "png", new File("E:/fl/参考资料/图例.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
