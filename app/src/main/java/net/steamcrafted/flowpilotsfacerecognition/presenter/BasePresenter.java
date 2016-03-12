package net.steamcrafted.flowpilotsfacerecognition.presenter;

/**
 * Created by Wannes2 on 12/03/2016.
 *
 * Base class for all Presenter classes in this application. Template class that takes the "Reaction"
 * interface as type. Reaction interface is used to talk from Presenter -> View. Where view is
 * usually an activity or a fragment.
 */
public class BasePresenter<T> {

    protected T mReact;

    /**
     * Create a new Presenter. The passed reactor will be used by the presenter to communicate
     * to the view that created the presenter.
     *
     * @param reactor The interface the presenter can use to talk to the view.
     */
    public BasePresenter(T reactor) {
        mReact = reactor;
    }
}
