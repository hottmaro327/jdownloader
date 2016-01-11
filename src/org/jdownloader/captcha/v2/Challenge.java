package org.jdownloader.captcha.v2;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class Challenge<T> {
    private final UniqueAlltimeID id           = new UniqueAlltimeID();
    private final Class<T>        resultType;
    private final long            created;
    private int                   timeout;
    private volatile boolean      accountLogin = false;
    private final boolean         createdInsideAccountChecker;

    public Object getAPIStorable(String format) throws Exception {
        return null;
    }

    public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
        final Plugin plg = getPlugin();
        if (plg != null) {
            if (plg instanceof PluginForHost) {
                return canBeSkippedByPluginforHost(skipRequest, solver, challenge, plg);
            } else if (plg instanceof PluginForDecrypt) {
                return canBeSkippedByPluginforDecrypt(skipRequest, solver, challenge, plg);
            }
        }
        return false;
    }

    private boolean canBeSkippedByPluginforDecrypt(final SkipRequest skipRequest, final ChallengeSolver<?> solver, final Challenge<?> challenge, final Plugin plugin) {
        final Plugin challengePlugin = challenge.getPlugin();
        if (challengePlugin == null || !(challengePlugin instanceof PluginForDecrypt)) {
            /* we only want block PluginForDecrypt captcha here */
            return false;
        }
        final PluginForDecrypt currentPlugin = (PluginForDecrypt) plugin;
        final LinkCrawler currentCrawler = currentPlugin.getCrawler();
        final CrawledLink currentOrigin = currentPlugin.getCurrentLink().getOriginLink();

        final PluginForDecrypt decrypt = (PluginForDecrypt) challengePlugin;
        if (currentCrawler != decrypt.getCrawler()) {
            /* we have a different crawler source */
            return false;
        }
        switch (skipRequest) {
        case STOP_CURRENT_ACTION:
            /* user wants to stop current action (eg crawling) */
            return true;
        case BLOCK_ALL_CAPTCHAS:
            /* user wants to block all captchas (current session) */
            return true;
        case BLOCK_HOSTER:
            /* user wants to block captchas from specific hoster */
            return StringUtils.equals(currentPlugin.getHost(), challengePlugin.getHost());
        case BLOCK_PACKAGE:
            final CrawledLink crawledLink = decrypt.getCurrentLink();
            return crawledLink != null && crawledLink.getOriginLink() == currentOrigin;
        default:
            return false;
        }
    }

    private boolean canBeSkippedByPluginforHost(final SkipRequest skipRequest, final ChallengeSolver<?> solver, final Challenge<?> challenge, final Plugin plugin) {
        if (isCreatedInsideAccountChecker() || isAccountLogin()) {
            /* we don't want to skip login captcha inside fetchAccountInfo(Thread is AccountCheckerThread) */
            return false;
        }
        final DownloadLink challengeLink = challenge.getDownloadLink();
        if (challengeLink == null) {
            return false;
        }
        final Plugin challengePlugin = challenge.getPlugin();
        if (challengePlugin != null && !(challengePlugin instanceof PluginForHost)) {
            /* we only want block PluginForHost captcha here */
            return false;
        }
        final PluginForHost currentPlugin = (PluginForHost) plugin;
        final DownloadLink currentLink = currentPlugin.getDownloadLink();

        switch (skipRequest) {
        case BLOCK_ALL_CAPTCHAS:
            /* user wants to block all captchas (current session) */
            return true;
        case BLOCK_HOSTER:
            /* user wants to block captchas from specific hoster */
            return StringUtils.equals(currentPlugin.getHost(), challengePlugin.getHost());
        case BLOCK_PACKAGE:
            /* user wants to block captchas from current FilePackage */
            if (challengeLink.getDefaultPlugin() == null) {
                return false;
            }
            final FilePackage currentFilePackage = currentLink.getFilePackage();
            return !FilePackage.isDefaultFilePackage(currentFilePackage) && challengeLink.getFilePackage() == currentLink.getFilePackage();
        default:
            return false;
        }
    }

    public boolean isAccountLogin() {
        return accountLogin;
    }

    // can be overridden to validate a response before adding it to the job
    public boolean validateResponse(AbstractResponse<T> response) {
        return true;
    }

    public void setAccountLogin(boolean accountLogin) {
        this.accountLogin = accountLogin;
    }

    public UniqueAlltimeID getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public Challenge(String method, String explain2) {
        typeID = method;
        explain = explain2;
        createdInsideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
        Type superClass = this.getClass().getGenericSuperclass();
        while (superClass instanceof Class) {

            superClass = ((Class<?>) superClass).getGenericSuperclass();
            if (superClass == null) {
                throw new IllegalArgumentException("Wrong Construct");
            }

        }
        resultType = (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
        created = System.currentTimeMillis();
        timeout = -1;
    }

    public boolean isCreatedInsideAccountChecker() {
        return createdInsideAccountChecker;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Class<T> getResultType() {
        return resultType;
    }

    abstract public boolean isSolved();

    private String typeID;
    private String explain;

    public String getTypeID() {
        return typeID;
    }

    public long getCreated() {
        return created;
    }

    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public ResponseList<T> getResult() {
        return result;
    }

    public void setResult(ResponseList<T> result) {
        this.result = result;
    }

    private ResponseList<T> result;

    public String getHost() {
        final Plugin plg = getPlugin();
        return plg == null ? null : plg.getHost();
    }

    public DownloadLink getDownloadLink() {
        final Plugin plugin = getPlugin();
        if (plugin != null && plugin instanceof PluginForHost) {
            return ((PluginForHost) plugin).getDownloadLink();
        }
        return null;
    }

    public DomainInfo getDomainInfo() {
        final Plugin plugin = getPlugin();
        if (plugin == null) {
            throw new WTFException("no plugin for this challenge!?");
        }
        DomainInfo ret = null;
        if (plugin instanceof PluginForHost) {
            final DownloadLink dl = ((PluginForHost) plugin).getDownloadLink();
            if (dl != null) {
                ret = dl.getDomainInfo();
            }
        }
        if (ret == null) {
            ret = DomainInfo.getInstance(plugin.getHost());
        }
        if (ret != null) {
            return ret;
        }
        throw new WTFException("no domaininfo for this challenge!?");
    }

    public Plugin getPlugin() {
        return null;
    }

    // Workaround until we implemented proper Refresh SUpport in the plugins
    public T getRefreshTrigger() {
        if (getResultType() == String.class) {
            return (T) "";
        }
        return null;
    }

    public AbstractResponse<T> parseAPIAnswer(String json, ChallengeSolver<?> solver) {
        return null;
    }
}
