package autoandshare.headvr.activity;

import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tencent.mmkv.MMKV;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.browse.HeadVRChannel;

public class StreamFragment extends Fragment {

    private MMKV historyStreams;
    private EditText urlText;
    private Button playButton;
    private TextView urlStatus;
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    private ListView mListView;

    private boolean checking = false;
    private String verifiedUrl = null;
    private String verifyInfo = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stream, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MMKV.initialize(getContext());
        historyStreams = MMKV.mmkvWithID("StreamUrls");

        String savedUrls = historyStreams.getString("history", "").trim();
        if (savedUrls.length() != 0) {
            listItems = new ArrayList<String>(Arrays.asList(
                    savedUrls.split("\\n")));
        }

        initUI(getView());
    }

    private String pastedUrl = null;

    @Override
    public void onResume() {
        super.onResume();
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            String text = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            if (URLUtil.isValidUrl(text) && !text.equals(pastedUrl)) {
                setUrlText(text);
                pastedUrl = text;
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        persistHistory();
    }

    private void persistHistory() {
        if (historyStreams != null) {
            historyStreams.putString("history", TextUtils.join("\n", listItems));
        }
    }

    private class HistoryAdapter extends ArrayAdapter<String> {
        HistoryAdapter() {
            super(StreamFragment.this.getContext(), 0, listItems);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(getContext())
                        .inflate(R.layout.stream_history_item, parent, false);
            }
            final String url = getItem(position);
            TextView urlView = row.findViewById(R.id.historyUrl);
            urlView.setText(url);
            View deleteButton = row.findViewById(R.id.historyDelete);
            deleteButton.setContentDescription("Remove " + url);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeUrl(url);
                }
            });
            return row;
        }
    }

    private void removeUrl(String url) {
        listItems.remove(url);
        adapter.notifyDataSetChanged();
        persistHistory();
    }

    private void initUI(View view) {
        checking = false;
        verifiedUrl = null;

        urlText = view.findViewById(R.id.streamUrl);
        playButton = view.findViewById(R.id.buttonPlayStream);
        urlStatus = view.findViewById(R.id.urlStatus);

        urlText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if ((verifiedUrl != null) && !s.toString().trim().equals(verifiedUrl)) {
                    verifiedUrl = null;
                    playButton.setText("Play");
                    urlStatus.setVisibility(View.GONE);
                }
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayPressed();
            }
        });

        Button channelButton = view.findViewById(R.id.buttonHeadvrChannel);
        channelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HeadVRChannel.setChannelURL(false);
                VlcHelper.openHeadVRChannel(getContext());
            }
        });
        channelButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                HeadVRChannel.setChannelURL(true);
                VlcHelper.openHeadVRChannel(getContext());
                return true;
            }
        });

        adapter = new HistoryAdapter();

        mListView = view.findViewById(R.id.streamUrlList);
        mListView.setAdapter(adapter);
        mListView.setEmptyView(view.findViewById(R.id.streamListEmpty));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openVideo(listItems.get(position));
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showHistoryItemDialog(position);
                return true;
            }
        });

        view.findViewById(R.id.clearUrlList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearHistory();
            }
        });

    }

    private void onPlayPressed() {
        final String url = urlText.getText().toString().trim();
        if (TextUtils.isEmpty(url) || checking) {
            return;
        }
        if (url.equals(verifiedUrl)) {
            openVideo(url);
            return;
        }
        startUrlCheck(url);
    }

    private void checkAndShowReady(String url) {
        setUrlText(url);
        startUrlCheck(url);
    }

    private void openVideo(String url) {
        setUrlText(url);
        addOrReplace(url);
        VlcHelper.openUri(getContext(), Uri.parse(url));
    }

    private void startUrlCheck(final String url) {
        checking = true;
        verifiedUrl = null;
        setStatus("Checking " + url, 0xFF888888);
        playButton.setEnabled(false);
        playButton.setText("Checking ...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String error = verifyUrl(url);
                final String info = verifyInfo;
                urlText.post(new Runnable() {
                    @Override
                    public void run() {
                        checking = false;
                        if (!isAdded() || (getView() == null)) {
                            return;
                        }
                        playButton.setEnabled(true);
                        if (error == null) {
                            verifiedUrl = url;
                            setStatus("Ready: " + info, 0xFF2E7D32);
                            playButton.setText("Ready - Play");
                        } else {
                            setStatus("Not ready: " + error, 0xFFC62828);
                            playButton.setText("Play");
                        }
                    }
                });
            }
        }).start();
    }

    private void setStatus(String text, int color) {
        urlStatus.setVisibility(View.VISIBLE);
        urlStatus.setText(text);
        urlStatus.setTextColor(color);
    }

    // returns null when the URL is usable, an error message otherwise
    private String verifyUrl(String url) {
        verifyInfo = "";
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null) {
            if (new File(url).exists()) {
                verifyInfo = "local file";
                return null;
            }
            return "file not found";
        }
        scheme = scheme.toLowerCase(Locale.US);
        if (scheme.equals("http") || scheme.equals("https")) {
            return checkHttp(url);
        }
        if (scheme.equals("file")) {
            if (new File(uri.getPath()).exists()) {
                verifyInfo = "local file";
                return null;
            }
            return "file not found";
        }
        if (scheme.equals("rtsp") || scheme.equals("rtmp")) {
            return checkSocket(uri.getHost(),
                    uri.getPort() > 0 ? uri.getPort() : (scheme.equals("rtsp") ? 554 : 1935));
        }
        verifyInfo = "not verified (" + scheme + ")";
        return null;
    }

    private String checkHttp(String url) {
        String error = request(url, true);
        if ((error != null) && error.startsWith("HTTP ")) {
            error = request(url, false);
        }
        return error;
    }

    private String request(String url, boolean head) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "VLC/3.4.0 LibVLC/3.4.0");
            if (head) {
                conn.setRequestMethod("HEAD");
            } else {
                conn.setRequestProperty("Range", "bytes=0-0");
            }
            int code = conn.getResponseCode();
            if ((code < 200) || (code > 399)) {
                return "HTTP " + code;
            }
            String type = conn.getContentType();
            if ((type != null) && type.toLowerCase(Locale.US).startsWith("text/html")) {
                return "web page instead of media (" + type + ")";
            }
            long length = -1;
            String lengthHeader = conn.getHeaderField("Content-Length");
            if (lengthHeader != null) {
                try {
                    length = Long.parseLong(lengthHeader.trim());
                } catch (NumberFormatException e) {
                }
            }
            verifyInfo = ((type == null) ? "media" : type.split(";")[0])
                    + ((length > 0) ? ", " + formatSize(length) : "");
            return null;
        } catch (Exception e) {
            return shortError(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String checkSocket(String host, int port) {
        if (TextUtils.isEmpty(host)) {
            return "bad URL";
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
            verifyInfo = "reachable " + host + ":" + port;
            return null;
        } catch (Exception e) {
            return shortError(e);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    private static String shortError(Exception e) {
        String message = e.getMessage();
        return ((message == null) || (message.length() == 0)) ?
                e.getClass().getSimpleName() : message;
    }

    private static String formatSize(long bytes) {
        if (bytes >= (1024L * 1024 * 1024)) {
            return String.format(Locale.US, "%.1f GB", bytes / 1073741824.0);
        }
        if (bytes >= (1024L * 1024)) {
            return String.format(Locale.US, "%.0f MB", bytes / 1048576.0);
        }
        if (bytes >= 1024) {
            return String.format(Locale.US, "%.0f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private void showHistoryItemDialog(final int position) {
        final String url = listItems.get(position);
        new AlertDialog.Builder(getContext())
                .setTitle(url)
                .setItems(new String[]{"Play now", "Check first", "Remove"}, (dialog, which) -> {
                    if (which == 0) {
                        openVideo(url);
                    } else if (which == 1) {
                        checkAndShowReady(url);
                    } else {
                        removeUrl(url);
                    }
                })
                .show();
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(getContext())
                .setTitle("Clear History")
                .setMessage("Remove all saved stream URLs?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    listItems.clear();
                    adapter.notifyDataSetChanged();
                    persistHistory();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setUrlText(String text) {
        urlText.setText(text);
        urlText.selectAll();
    }

    private void addOrReplace(String url) {
        listItems.remove(url);
        listItems.add(0, url);
        if (listItems.size() >= 100) {
            listItems.remove(listItems.size() - 1);
        }
        adapter.notifyDataSetChanged();
        persistHistory();
    }
}
