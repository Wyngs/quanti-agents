package com.quantiagents.app.domain.services;

/**
 * Service for managing lottery selection criteria and descriptions.
 */
public interface LotteryService {

    /**
     * @return organizer-defined selection criteria text
     */
    String getSelectionCriteria();

    /**
     * @return default criteria text if no custom criteria is available
     */
    String getDefaultCriteria();
}
