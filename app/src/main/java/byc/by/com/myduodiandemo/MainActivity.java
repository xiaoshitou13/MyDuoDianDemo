package byc.by.com.myduodiandemo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private int total = 0;
    private boolean downloading = false;
    private boolean downloadings = false;
    private URL url;
    private File file;

    private int length;

    /**
     * Button
     */
    private Button mButton;

    private ProgressBar mProgressBar;
    /**
     * 下载：
     */
    private TextView mTv;
    /**
     * Button
     */
    private Button mButton3;
    /**
     * TextView
     */
    private    List<HashMap<String, Integer>> threadList;
    private TextView mTextView;
    private ProgressBar mProgressBar2;
//    String fileName = "weixin_821.apk";
//    String u2 = "http://gdown.baidu.com/data/wisegame/df65a597122796a4/" + fileName;
    String u = "http://abv.cn/music/%E5%85%89%E8%BE%89%E5%B2%81%E6%9C%88.mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);

        threadList = new ArrayList<>();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mTv = (TextView) findViewById(R.id.tv);

        mProgressBar.setOnClickListener(this);
        mButton3 = (Button) findViewById(R.id.button3);
        mButton3.setOnClickListener(this);
        mTextView = (TextView) findViewById(R.id.textView);
        mProgressBar2 = (ProgressBar) findViewById(R.id.progressBar2);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.button:

                if (downloading) {
                    downloading = false;
                    mButton.setText("下载");

                    return;
                }
                downloading = true;
                mButton.setText("暂停");


                if (threadList.size() == 0) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                url = new URL(u);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setConnectTimeout(5000);
                                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727)");
                                length = connection.getContentLength();

                                mProgressBar.setMax(length);

                                mProgressBar.setProgress(0);

                                if (length < 0) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "File not found !", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    return;
                                }

                                file = new File(Environment.getExternalStorageDirectory(), getFileName(u));
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                randomAccessFile.setLength(length);

                                int blockSize = length / 3;
                                for (int i = 0; i < 3; i++) {
                                    int begin = i * blockSize;
                                    int end = (i + 1) * blockSize - 1;
                                    if (i == 2) {
                                        end = length;
                                    }

                                    HashMap<String, Integer> map = new HashMap<>();
                                    map.put("begin", begin);
                                    map.put("end", end);
                                    map.put("finished", 0);
                                    threadList.add(map);

                                    //创建线程 下载文件
                                    new Thread(new DownloadRunnable(begin, end, i, file, url)).start();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    //恢复下载
                    for (int i = 0; i < threadList.size(); i++) {
                        HashMap<String, Integer> map = threadList.get(i);
                        int begin = map.get("begin");
                        int end = map.get("end");
                        int finished = map.get("finished");
                        new Thread(new DownloadRunnable(begin + finished, end, i, file, url)).start();
                    }
                }
                break;



            case R.id.button3:

                break;

        }
    }
    private String getFileName(String url) {
        int index = url.lastIndexOf("/") + 1;
        return url.substring(index);
    }


    class DownloadRunnable implements Runnable {

        private int begin;
        private int end;
        private int id;
        private File file;
        private URL url;


        public DownloadRunnable(int begin, int end, int id, File file, URL url) {
            this.begin = begin;
            this.end = end;
            this.id = id;
            this.file = file;
            this.url = url;
        }

        @Override
        public void run() {
            try {
                if (begin > end) {
                    return;
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727)");
                connection.setRequestProperty("Range", "bytes=" + begin + "-" + end);

                InputStream is = connection.getInputStream();
                byte[] buf = new byte[1024 * 1024];
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                randomAccessFile.seek(begin);
                int len;
                HashMap<String, Integer> map = threadList.get(id);
                while ((len = is.read(buf)) != -1 && downloading) {
                    randomAccessFile.write(buf, 0, len);
                    updateProgress(len);
                    map.put("finished", map.get("finished") + len);
                    System.out.println("Download:" + total);
                }
                is.close();
                randomAccessFile.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


     private void updateProgress(final int len) {
        total += len;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mProgressBar.setProgress(total);
                mTv.setText("下载：" + total * 100 / length + "%");
                if (total == length) {
                    Toast.makeText(MainActivity.this, "下载完成！", Toast.LENGTH_SHORT).show();
                    //total = 0;
                    mButton.setText("完成");
                }
            }
        });
    }

}
