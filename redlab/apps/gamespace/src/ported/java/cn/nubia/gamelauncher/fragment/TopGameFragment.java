package dev.redlab.gamespace.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.fragment.TopGameFragment;
import dev.redlab.gamespace.util.NetworkStateMonitor;
import cn.nubia.neostore.gameplace.GameListView;
import cn.nubia.neostore.gameplace.popup.PopupCallbacks;
import cn.nubia.neostore.gameplace.popup.PopupConfig;
import cn.nubia.neostore.gameplace.popup.PopupManager;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/* loaded from: classes.dex */
public class TopGameFragment extends Fragment {
    private static final String TAG = "TopGameFragment";
    GameListView gameListView;
    private PopupManager mPopupManager;
    View mView;
    private NetworkStateMonitor networkMonitor;

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.top_game_layout, viewGroup, false);
        this.mView = inflate;
        this.gameListView = (GameListView) inflate.findViewById(R.id.game_list_view);
        this.mPopupManager = new PopupManager(getContext());
        return this.mView;
    }

    @Override // androidx.fragment.app.Fragment
    public void onStart() {
        super.onStart();
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        requestPopup();
    }

    @Override // androidx.fragment.app.Fragment
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()");
    }

    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach()");
    }

    @Override // androidx.fragment.app.Fragment
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach()");
    }

    private void requestPopup() {
        PopupManager popupManager = this.mPopupManager;
        if (popupManager != null) {
            popupManager.requestPopup(new Function1() { // from class: dev.redlab.gamespace.fragment.TopGameFragment$$ExternalSyntheticLambda1
                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj) {
                    return TopGameFragment.this.m248xc56896f7((Boolean) obj);
                }
            });
        }
    }

    /* renamed from: lambda$requestPopup$0$cn-nubia-gamelauncher-fragment-TopGameFragment, reason: not valid java name */
    /* synthetic */ Unit m248xc56896f7(Boolean bool) {
        Log.d(TAG, "requestPopup hasPopup =" + bool);
        if (bool.booleanValue()) {
            showPopup();
        }
        return Unit.INSTANCE;
    }

    private void showPopup() {
        PopupManager popupManager = this.mPopupManager;
        if (popupManager == null || popupManager.isPopupShowing()) {
            return;
        }
        PopupCallbacks popupCallbacks = new PopupCallbacks(null, null, null, null, new Function0() { // from class: dev.redlab.gamespace.fragment.TopGameFragment$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit onCloseButtonClick;
                onCloseButtonClick = TopGameFragment.this.onCloseButtonClick();
                return onCloseButtonClick;
            }
        });
        PopupConfig build = new PopupConfig.Builder().width(-2).heightPercent(0.74f).gravity(17).build();
        if (this.mView != null) {
            Log.d(TAG, "popupManager.showPopup()");
            this.mPopupManager.showPopup(this.mView, build, popupCallbacks);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Unit onCloseButtonClick() {
        Log.d(TAG, "Popup onCloseButtonClick");
        PopupManager popupManager = this.mPopupManager;
        if (popupManager != null) {
            popupManager.dismissPopup();
            this.mPopupManager = null;
        }
        return Unit.INSTANCE;
    }

    @Override // androidx.fragment.app.Fragment
    public void onHiddenChanged(boolean z) {
        Log.d(TAG, "onHiddenChanged");
        super.onHiddenChanged(z);
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, "onCreate");
        if (getActivity() != null) {
            this.networkMonitor = new NetworkStateMonitor(getActivity(), new AnonymousClass1());
        }
    }

    /* renamed from: dev.redlab.gamespace.fragment.TopGameFragment$1, reason: invalid class name */
    class AnonymousClass1 implements NetworkStateMonitor.OnNetworkStateChangeListener {
        AnonymousClass1() {
        }

        @Override // dev.redlab.gamespace.util.NetworkStateMonitor.OnNetworkStateChangeListener
        public void onNetworkAvailable(final boolean z) {
            if (TopGameFragment.this.getActivity() != null) {
                TopGameFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: dev.redlab.gamespace.fragment.TopGameFragment$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        TopGameFragment.AnonymousClass1.this.m249xecc40f87(z);
                    }
                });
            }
        }

        /* renamed from: lambda$onNetworkAvailable$0$cn-nubia-gamelauncher-fragment-TopGameFragment$1, reason: not valid java name */
        /* synthetic */ void m249xecc40f87(boolean z) {
            Log.d(TopGameFragment.TAG, "onNetworkAvailable  isNetworkAvailable =" + z);
            TopGameFragment.this.gameListViewReload();
        }
    }

    public void gameListViewReload() {
        GameListView gameListView = this.gameListView;
        if (gameListView != null) {
            gameListView.testReload();
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        NetworkStateMonitor networkStateMonitor = this.networkMonitor;
        if (networkStateMonitor != null) {
            networkStateMonitor.stopMonitoring();
            this.networkMonitor = null;
        }
    }
}
