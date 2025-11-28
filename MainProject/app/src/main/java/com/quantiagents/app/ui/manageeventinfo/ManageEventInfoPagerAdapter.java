package com.quantiagents.app.ui.manageeventinfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Pager adapter for the manage-event-info screen. Provides four pages
 * (WAITING, SELECTED, CONFIRMED, CANCELED) scoped to the same event.
 */
public class ManageEventInfoPagerAdapter extends FragmentStateAdapter {

    private final String eventId;

    /**
     * Constructs the adapter using the host fragment (so it uses the host's child FragmentManager).
     *
     * @param host    Fragment host that owns this adapter.
     * @param eventId Event identifier to pass to each page.
     */
    public ManageEventInfoPagerAdapter(@NonNull Fragment host, @NonNull String eventId) {
        super(host);
        this.eventId = eventId;
    }

    /** Creates the page fragment for a given position. */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        final String status = switch (position) {
            case 1 -> "SELECTED";
            case 2 -> "CONFIRMED";
            case 3 -> "CANCELED";
            default -> "WAITLIST";
        };
        return ManageEventInfoListFragment.newInstance(eventId, status);
    }

    /** @return Fixed page count: 4 statuses. */
    @Override
    public int getItemCount() { return 4; }
}