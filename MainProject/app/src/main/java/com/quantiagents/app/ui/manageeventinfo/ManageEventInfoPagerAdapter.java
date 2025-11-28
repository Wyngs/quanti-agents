package com.quantiagents.app.ui.manageeventinfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Pager adapter for the Manage Event Info screen.
 *
 * It exposes four pages, one for each registration status:
 * <ol>
 *     <li>WAITLIST</li>
 *     <li>SELECTED</li>
 *     <li>CONFIRMED</li>
 *     <li>CANCELLED</li>
 * </ol>
 *
 * Each page is an instance of {@link ManageEventInfoListFragment}.
 */
public class ManageEventInfoPagerAdapter extends FragmentStateAdapter {

    private final String eventId;

    /**
     * Creates a new adapter for the given event.
     *
     * @param host    parent fragment hosting the ViewPager2.
     * @param eventId ID of the event whose registrations we are displaying.
     */
    public ManageEventInfoPagerAdapter(@NonNull Fragment host, @NonNull String eventId) {
        super(host);
        this.eventId = eventId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // NOTE: Strings here must exactly match the enum names in constant.EventRegistrationStatus.
        final String status = switch (position) {
            case 1 -> "SELECTED";
            case 2 -> "CONFIRMED";
            case 3 -> "CANCELED";
            default -> "WAITLIST";
        };
        return ManageEventInfoListFragment.newInstance(eventId, status);
    }

    /**
     * @return fixed page count – one for each status.
     */
    @Override
    public int getItemCount() {
        return 4;
    }
}
