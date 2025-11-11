<?php
/**
 * Logout API Endpoint
 */

session_start();

// Destroy session
session_unset();
session_destroy();

sendJSONResponse(null, 200, 'Logout successful');

