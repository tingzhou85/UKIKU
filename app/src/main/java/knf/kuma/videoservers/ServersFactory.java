package knf.kuma.videoservers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import knf.kuma.App;
import knf.kuma.BuildConfig;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.custom.DialogWraper;
import knf.kuma.database.CacheDB;
import knf.kuma.download.DownloadManager;
import knf.kuma.download.DownloadService;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.QueueObject;
import knf.kuma.queue.QueueManager;
import xdroid.toaster.Toaster;

public class ServersFactory {
    private Context context;
    private FragmentManager fragmentManager;
    private String url;
    private AnimeObject.WebInfo.AnimeChapter chapter;
    private DownloadObject downloadObject;
    private boolean isStream;
    private ServersInterface serversInterface;

    private List<Server> servers = new ArrayList<>();
    private int selected = 0;

    private ServersFactory(Context context, FragmentManager fragmentManager, String url, AnimeObject.WebInfo.AnimeChapter chapter, boolean isStream, boolean addQueue, ServersInterface serversInterface) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.url = url;
        this.chapter = chapter;
        this.downloadObject = DownloadObject.fromChapter(chapter, addQueue);
        this.isStream = isStream;
        this.serversInterface = serversInterface;
    }

    private ServersFactory(Context context, FragmentManager fragmentManager, String url, DownloadObject downloadObject, boolean isStream, ServersInterface serversInterface) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.url = url;
        this.downloadObject = downloadObject;
        this.isStream = isStream;
        this.serversInterface = serversInterface;
    }

    public static void start(final Context context, FragmentManager fragmentManager, final String url, final AnimeObject.WebInfo.AnimeChapter chapter, final boolean isStream, final boolean addQueue, final ServersInterface serversInterface) {
        try {
            final DialogWraper dialog = DialogWraper.wrap(new MaterialDialog.Builder(context)
                    .content("Obteniendo servidores")
                    .progress(true, 0)
                    .build());
            dialog.show(fragmentManager, "getting servers");
            AsyncTask.execute(() -> {
                ServersFactory factory = new ServersFactory(context, fragmentManager, url, chapter, isStream, addQueue, serversInterface);
                factory.get(dialog);
            });
        } catch (Exception e) {
            //
        }
    }

    public static void start(final Context context, FragmentManager fragmentManager, final String url, final AnimeObject.WebInfo.AnimeChapter chapter, final boolean isStream, final ServersInterface serversInterface) {
        start(context, fragmentManager, url, chapter, isStream, false, serversInterface);
    }

    public static void start(final Context context, FragmentManager fragmentManager, final String url, final DownloadObject downloadObject, final boolean isStream, final ServersInterface serversInterface) {
        try {
            final DialogWraper dialog = DialogWraper.wrap(new MaterialDialog.Builder(context)
                    .content("Obteniendo servidores")
                    .progress(true, 0)
                    .build());
            dialog.show(fragmentManager, "getting servers");
            AsyncTask.execute(() -> {
                ServersFactory factory = new ServersFactory(context, fragmentManager, url, downloadObject, isStream, serversInterface);
                factory.get(dialog);
            });
        } catch (Exception e) {
            //
        }
    }

    public static void startPlay(Context context, String title, String file_name) {
        File file = FileAccessHelper.INSTANCE.getFile(file_name);
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0").equals("0")) {
            context.startActivity(PrefsUtil.INSTANCE.getPlayerIntent()
                    .setData(Uri.fromFile(file))
                    .putExtra("isFile", true)
                    .putExtra("title", title));
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, FileAccessHelper.INSTANCE.getDataUri(file_name))
                    .setDataAndType(FileAccessHelper.INSTANCE.getDataUri(file_name), "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .putExtra("title", title);
            context.startActivity(intent);
        }
    }

    private static String getEpTitle(String title, String file) {
        return title + " " + file.substring(file.lastIndexOf("-") + 1, file.lastIndexOf("."));
    }

    public static PendingIntent getPlayIntent(Context context, String title, String file_name) {
        File file = FileAccessHelper.INSTANCE.getFile(file_name);
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0").equals("0")) {
            return PendingIntent.getActivity(context, Math.abs(file_name.hashCode()),
                    PrefsUtil.INSTANCE.getPlayerIntent()
                            .setData(Uri.fromFile(file)).putExtra("isFile", true)
                            .putExtra("title", getEpTitle(title, file_name))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, FileAccessHelper.INSTANCE.getDataUri(file_name))
                    .setDataAndType(FileAccessHelper.INSTANCE.getDataUri(file_name), "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("title", getEpTitle(title, file_name));
            return PendingIntent.getActivity(context, Math.abs(file_name.hashCode()), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private static void safeDismiss(DialogWraper dialog) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            //
        }
    }

    private static void safeDismiss(MaterialDialog dialog) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            //
        }
    }

    private void showServerList() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (servers.size() == 0) {
                    Toaster.toast("Sin servidores disponibles");
                    callOnFinish(false, false);
                } else {
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                            .title("Selecciona servidor")
                            .items(Server.getNames(servers))
                            .autoDismiss(false)
                            .itemsCallbackSingleChoice(selected, (d, itemView, which, text) -> {
                                selected = which;
                                safeDismiss(d);
                                final DialogWraper dialog = DialogWraper.wrap(new MaterialDialog.Builder(context)
                                        .content("Obteniendo link")
                                        .progress(true, 0)
                                        .cancelable(false)
                                        .build());
                                safeShow(dialog, "getting link");
                                AsyncTask.execute(() -> {
                                    try {
                                        final VideoServer server = servers.get(selected).getVerified();
                                        safeDismiss(dialog);
                                        if (server == null && servers.size() == 1) {
                                            Toaster.toast("Error en servidor, intente mas tarde");
                                            callOnFinish(false, false);
                                        } else if (server == null) {
                                            servers.remove(selected);
                                            selected = 0;
                                            Toaster.toast("Error en servidor");
                                            showServerList();
                                        } else if (server.options.size() == 0) {
                                            servers.remove(selected);
                                            selected = 0;
                                            Toaster.toast("Error en servidor");
                                            showServerList();
                                        } else if (server.haveOptions()) {
                                            showOptions(server, false);
                                        } else {
                                            switch (text.toString().toLowerCase()) {
                                                case "mega 1":
                                                case "mega 2":
                                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(server.getOption().url)));
                                                    callOnFinish(false, false);
                                                    break;
                                                default:
                                                    if (isStream)
                                                        startStreaming(server.getOption());
                                                    else
                                                        startDownload(server.getOption());
                                                    break;
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                                return true;
                            }).positiveText(downloadObject.addQueue ? "AÑADIR" : "INICIAR")
                            .negativeText("CANCELAR")
                            .onNegative((dialog, which) -> callOnFinish(false, false))
                            .onAny((dialog, which) -> safeDismiss(dialog))
                            .cancelListener(dialog -> callOnFinish(false, false));
                    if (isStream && CastUtil.get().connected())
                        builder.neutralText("CAST")
                                .onNeutral((d, which) -> {
                                    selected = d.getSelectedIndex();
                                    safeDismiss(d);
                                    final DialogWraper dialog = DialogWraper.wrap(new MaterialDialog.Builder(context)
                                            .content("Obteniendo link")
                                            .progress(true, 0)
                                            .build());
                                    safeShow(dialog, "getting links");
                                    AsyncTask.execute(() -> {
                                        try {
                                            final VideoServer server = servers.get(selected).getVerified();
                                            safeDismiss(dialog);
                                            if (server == null && servers.size() == 1) {
                                                Toaster.toast("Error en servidor, intente mas tarde");
                                                callOnFinish(false, false);
                                            } else if (server == null) {
                                                Toaster.toast("Error en servidor");
                                                showServerList();
                                            } else if (server.haveOptions()) {
                                                showOptions(server, true);
                                            } else {
                                                switch (Server.getNames(servers).get(d.getSelectedIndex()).toLowerCase()) {
                                                    case "mega 1":
                                                    case "mega 2":
                                                    case "zippyshare":
                                                        Toaster.toast("No soportado en CAST");
                                                        showServerList();
                                                        break;
                                                    default:
                                                        callOnCast(server.getOption().url);
                                                        break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                });
                    safeShow(DialogWraper.wrap(builder.build()), "server selection");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void showOptions(final VideoServer server, final boolean isCast) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                DialogWraper.wrap(new MaterialDialog.Builder(context)
                        .title(server.name)
                        .items(Option.getNames(server.options))
                        .autoDismiss(false)
                        .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
                            safeDismiss(dialog);
                            if (isCast) {
                                callOnCast(server.options.get(which).url);
                            } else if (isStream) {
                                startStreaming(server.options.get(which));
                            } else {
                                startDownload(server.options.get(which));
                            }
                            return true;
                        })
                        .positiveText(downloadObject.addQueue ? "AÑADIR" : "INICIAR")
                        .negativeText("ATRAS")
                        .onAny((dialog, which) -> safeDismiss(dialog))
                        .cancelListener(dialog -> showServerList()).build()).show(fragmentManager, "options");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void get(DialogWraper dialog) {
        try {
            Document main = Jsoup.connect(url).timeout(5000).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get();
            Elements descargas = main.select("table.RTbl.Dwnl").first().select("a.Button.Sm.fa-download");
            List<Server> servers = new ArrayList<>();
            for (Element e : descargas) {
                String z = e.attr("href");
                z = z.substring(z.lastIndexOf("http"));
                Server server = Server.check(context, z);
                if (server != null)
                    servers.add(server);
            }
            Elements s_script = main.select("script");
            String j = "";
            for (Element element : s_script) {
                String s_el = element.outerHtml();
                if (s_el.contains("var video = [];")) {
                    j = s_el;
                    break;
                }
            }
            String[] parts = j.substring(j.indexOf("var video = [];") + 14, j.indexOf("$(document).ready(function()")).split("video\\[[^a-z]*\\]");
            for (String baseLink : parts) {
                Server server = Server.check(context, baseLink);
                if (server != null)
                    servers.add(server);
            }
            Collections.sort(servers);
            this.servers = servers;
            safeDismiss(dialog);
            showServerList();
        } catch (Exception e) {
            e.printStackTrace();
            this.servers = new ArrayList<>();
            safeDismiss(dialog);
            callOnFinish(false, false);
        }
    }

    private void startStreaming(Option option) {
        if (chapter != null && downloadObject.addQueue) {
            QueueManager.add(Uri.parse(option.url), false, chapter);
        } else {
            Answers.getInstance().logCustom(new CustomEvent("Streaming").putCustomAttribute("Server", option.server));
            if (PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString("player_type", "0").equals("0")) {
                App.getContext().startActivity(
                        PrefsUtil.INSTANCE.getPlayerIntent()
                                .setData(Uri.parse(option.url))
                                .putExtra("title", downloadObject.title)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(option.url), "video/mp4")
                        .putExtra("title", downloadObject.title)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                App.getContext().startActivity(intent);
            }
        }
        callOnFinish(false, true);
    }

    private void startDownload(Option option) {
        if (BuildConfig.DEBUG) Log.e("Download " + option.server, option.url);
        if (chapter != null && CacheDB.INSTANCE.queueDAO().isInQueue(chapter.eid))
            CacheDB.INSTANCE.queueDAO().add(new QueueObject(Uri.fromFile(FileAccessHelper.INSTANCE.getFile(chapter.getFileName())), true, chapter));
        Answers.getInstance().logCustom(new CustomEvent("Download").putCustomAttribute("Server", option.server));
        downloadObject.link = option.url;
        downloadObject.headers = option.headers;
        if (PrefsUtil.INSTANCE.getDownloaderType() == 0) {
            CacheDB.INSTANCE.downloadsDAO().insert(downloadObject);
            ContextCompat.startForegroundService(App.getContext(), new Intent(App.getContext(), DownloadService.class).putExtra("eid", downloadObject.eid).setData(Uri.parse(option.url)));
            callOnFinish(true, true);
        } else
            callOnFinish(true, DownloadManager.start(downloadObject));
    }

    private void safeShow(DialogWraper dialog, String tag) {
        try {
            dialog.show(fragmentManager, tag);
        } catch (Exception e) {
            //
        }
    }

    private void safeShow(MaterialDialog dialog) {
        try {
            dialog.show();
        } catch (Exception e) {
            //
        }
    }

    private void callOnFinish(boolean started, boolean success) {
        if (serversInterface != null)
            serversInterface.onFinish(started, success);
    }

    private void callOnCast(String url) {
        if (serversInterface != null)
            serversInterface.onCast(url);
    }

    public interface ServersInterface {
        void onFinish(boolean started, boolean success);

        void onCast(String url);
    }
}
