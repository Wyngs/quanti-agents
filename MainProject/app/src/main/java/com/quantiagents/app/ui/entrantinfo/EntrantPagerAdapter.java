package com.quantiagents.app.ui.entrantinfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Pager adapter for the Entrant Information screen.
 * <p>
 * Provides four pages to {@link androidx.viewpager2.widget.ViewPager2}:
 * <ol>
 *   <li>WAITING</li>
 *   <li>SELECTED</li>
 *   <li>CONFIRMED</li>
 *   <li>CANCELED</li>
 * </ol>
 * Each page is an {@link EntrantListFragment} scoped to the same event but a different status.
 */
public class EntrantPagerAdapter extends FragmentStateAdapter {

    private final String eventId;

    /**
     * Constructs the adapter using the host fragment (so it uses the host's child FragmentManager).
     *
     * @param host    Fragment host that owns this adapter.
     * @param eventId Event identifier to pass to each page.
     */
    public EntrantPagerAdapter(@NonNull Fragment host, @NonNull String eventId) {
        super(host);
        this.eventId = eventId;
    }

    /**
     * Creates the page fragment for a given position.
     *
     * @param position 0..3 mapped to WAITING, SELECTED, CONFIRMED, CANCELED.
     * @return A new {@link EntrantListFragment} with proper status argument.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        final String status = switch (position) {
            case 1 -> "SELECTED";
            case 2 -> "CONFIRMED";
            case 3 -> "CANCELED";
            default -> "WAITING";
        };
        return EntrantListFragment.newInstance(eventId, status);
    }

    /** @return Fixed page count: 4 statuses. */
    @Override
    public int getItemCount() {
        return 4;
    }
}
