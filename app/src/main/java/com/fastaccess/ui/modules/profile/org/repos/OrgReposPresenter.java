package com.fastaccess.ui.modules.profile.org.repos;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.fastaccess.data.dao.NameParser;
import com.fastaccess.data.dao.model.Repo;
import com.fastaccess.helper.RxHelper;
import com.fastaccess.provider.rest.RestProvider;
import com.fastaccess.ui.base.mvp.presenter.BasePresenter;
import com.fastaccess.ui.modules.repos.RepoPagerActivity;

import java.util.ArrayList;

/**
 * Created by Kosh on 03 Dec 2016, 3:48 PM
 */

class OrgReposPresenter extends BasePresenter<OrgReposMvp.View> implements OrgReposMvp.Presenter {

    private ArrayList<Repo> repos = new ArrayList<>();
    private int page;
    private int previousTotal;
    private int lastPage = Integer.MAX_VALUE;

    @Override public int getCurrentPage() {
        return page;
    }

    @Override public int getPreviousTotal() {
        return previousTotal;
    }

    @Override public void setCurrentPage(int page) {
        this.page = page;
    }

    @Override public void setPreviousTotal(int previousTotal) {
        this.previousTotal = previousTotal;
    }

    @Override public void onError(@NonNull Throwable throwable) {
        sendToView(view -> {
            if (view.getLoadMore().getParameter() != null) {
                onWorkOffline(view.getLoadMore().getParameter());
            }
        });
        super.onError(throwable);
    }

    @Override public void onCallApi(int page, @Nullable String parameter) {
        if (parameter == null) {
            throw new NullPointerException("Username is null");
        }
        if (page == 1) {
            lastPage = Integer.MAX_VALUE;
            sendToView(view -> view.getLoadMore().reset());
        }
        setCurrentPage(page);
        if (page > lastPage || lastPage == 0) {
            sendToView(OrgReposMvp.View::hideProgress);
            return;
        }
        makeRestCall(RestProvider.getOrgService().getOrgRepos(parameter, page),
                repoModelPageable -> {
                    lastPage = repoModelPageable.getLast();
                    if (getCurrentPage() == 1) {
                        manageObservable(Repo.saveMyRepos(repoModelPageable.getItems(), parameter));
                    }
                    sendToView(view -> view.onNotifyAdapter(repoModelPageable.getItems(), page));
                });
    }

    @NonNull @Override public ArrayList<Repo> getRepos() {
        return repos;
    }

    @Override public void onWorkOffline(@NonNull String login) {
        if (repos.isEmpty()) {
            manageDisposable(RxHelper.getObserver(Repo.getMyRepos(login).toObservable()).subscribe(repoModels ->
                    sendToView(view -> view.onNotifyAdapter(repoModels, 1))));
        } else {
            sendToView(OrgReposMvp.View::hideProgress);
        }
    }

    @Override public void onItemClick(int position, View v, Repo item) {
        RepoPagerActivity.startRepoPager(v.getContext(), new NameParser(item.getHtmlUrl()));
    }

    @Override public void onItemLongClick(int position, View v, Repo item) {}
}
