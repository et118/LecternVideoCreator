public class Main {
    public static void main(String[] args) {
        if(args.length <  2) {
            System.out.println("Specify video URL and Name to video file");
            return;
        }
        try {
            VideoCreator.createVideoFromURL(args[0],args[1],28,14);
            Runnable statusThread = new Runnable() {
                @Override
                public void run() {
                    int lastProgress = 0;
                    VideoCreator.State lastState = VideoCreator.State.Nothing;
                    while(true) {
                        if(lastState != VideoCreator.state) {
                            System.out.println(VideoCreator.state.name());
                            lastState = VideoCreator.state;
                        }
                        if(lastProgress != VideoCreator.progress) {
                            System.out.println(VideoCreator.progress + "%");
                            lastProgress = VideoCreator.progress;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if(lastState == VideoCreator.State.Done) break;
                    }
                }
            };
            new Thread(statusThread).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to add video");
        }
    }
}
