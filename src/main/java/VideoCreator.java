import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;

public class VideoCreator {
    public enum State{
        Nothing,
        Downloading,
        Converting,
        Done
    }
    public static State state = State.Nothing;
    public static int progress = 0;
    private static double fps = 20;
    public static void createVideoFromURL(String URL,String name ,int width, int height) throws IOException, URISyntaxException {
        URI executableFolder = Paths.get(System.getProperty("user.dir")).toUri();
        System.out.println(executableFolder);
        if(state != State.Nothing) return;
        String videoName = name;
        File folder = new File(executableFolder.resolve("Videos"));
        File downloadFile = new File(executableFolder.resolve("Videos/" + videoName + "." + URL.split("\\.")[URL.split("\\.").length-1]));
        File videoFile = new File(executableFolder.resolve("Videos/" + videoName + ".et118"));
        if(!folder.exists()) folder.mkdirs();
        if(videoFile.exists()) {
            progress = 100;
            state = State.Done;
            return;
        }
        Runnable downloadThread = new Runnable() {
            @Override
            public void run() {
                try {
                    progress = 0;
                    state = State.Downloading;
                    if(!downloadFile.exists()) {
                        downloadFile.createNewFile();
                        URL url = new URL(URL);
                        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                        long totalFileSize = httpConnection.getContentLength();
                        int chunkSize = 5096;
                        BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(downloadFile), chunkSize);
                        byte[] data = new byte[chunkSize];
                        long downloadedSize = 0;
                        int i = 0;
                        while ((i = inputStream.read(data, 0, chunkSize)) >= 0) {
                            downloadedSize += i;
                            progress = (int) ((((double) downloadedSize) / ((double) totalFileSize)) * 100d);
                            bufferedOutputStream.write(data, 0, i);
                        }
                        bufferedOutputStream.close();
                        inputStream.close();
                    }
                    progress = 0;
                    state = State.Converting;
                    if(!videoFile.exists()) {
                        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(downloadFile);
                        Java2DFrameConverter converter = new Java2DFrameConverter();
                        grabber.setImageWidth(width);
                        grabber.setImageHeight(height);
                        grabber.setImageMode(FrameGrabber.ImageMode.GRAY);
                        //grabber.setFrameRate(fps);  Doesn't even work. Piece of sh*t
                        grabber.start();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Frame frame;
                        double frameCount = 0;
                        int savedFrames = 0;
                        int estimatedLength = grabber.getLengthInFrames();
                        while((frame = grabber.grabImage()) != null) {
                            frameCount+=fps/grabber.getFrameRate();
                            if((int)frameCount > savedFrames) {
                                BufferedImage bufferedImage = converter.convert(frame);
                                for(int y = 0; y < height; y++) {
                                    for(int x = 0; x < width; x++) {
                                        int[] rawPixel = new int[1];
                                        bufferedImage.getData().getPixel(x,y,rawPixel);
                                        baos.write(rawPixel[0]);
                                    }
                                }
                                progress = (int) ((frameCount / ((double) estimatedLength)) * 100d);
                                savedFrames++;
                            }
                        }
                        baos.flush();
                        grabber.flush();
                        baos.close();
                        grabber.stop();
                        Video video = new Video(videoName,width,height,baos.toByteArray());
                        video.saveToFile();
                        downloadFile.delete();
                    }
                    progress = 100;
                    state = State.Done;
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(downloadThread).start();
    }
}