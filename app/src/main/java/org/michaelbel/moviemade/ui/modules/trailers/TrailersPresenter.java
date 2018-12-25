package org.michaelbel.moviemade.ui.modules.trailers;

import org.jetbrains.annotations.NotNull;
import org.michaelbel.moviemade.data.entity.Video;
import org.michaelbel.moviemade.utils.EmptyViewMode;
import org.michaelbel.moviemade.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class TrailersPresenter implements TrailersContract.Presenter {

    private TrailersContract.View view;
    private TrailersContract.Repository repository;
    private CompositeDisposable disposables = new CompositeDisposable();

    public TrailersPresenter(TrailersRepository repository) {
        this.repository = repository;
    }

    @Override
    public void setView(@NotNull TrailersContract.View view) {
        this.view = view;
    }

    @Override
    public void getVideos(int movieId) {
        // Fixme.
        if (NetworkUtil.INSTANCE.notConnected()) {
            view.setError(EmptyViewMode.MODE_NO_CONNECTION);
            return;
        }

        disposables.add(repository.getVideos(movieId)
            .doOnTerminate(() -> view.hideLoading())
            .subscribe(videosResponse -> {
                // Fixme.
                List<Video> results = new ArrayList<>(videosResponse.getTrailers());
                if (results.isEmpty()) {
                    view.setError(EmptyViewMode.MODE_NO_TRAILERS);
                    return;
                }
                view.setTrailers(videosResponse.getTrailers());
            }, throwable -> view.setError(EmptyViewMode.MODE_NO_TRAILERS)));
    }

    @Override
    public void onDestroy() {
        disposables.dispose();
    }
}