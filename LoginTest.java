package com.qaportfolio.tests;

import com.qaportfolio.base.BaseTest;
import com.qaportfolio.pages.DashboardPage;
import com.qaportfolio.pages.LoginPage;
import com.qaportfolio.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    // ── TC01: Valid Login ──────────────────────────────────
    @Test(priority = 1, description = "Verify successful login with valid credentials")
    public void testValidLogin() {
        ExtentReportManager.createTest("TC01 - Valid Login");

        LoginPage loginPage = new LoginPage(getDriver());
        DashboardPage dashboard = loginPage.login(
            "testuser@example.com",
            "Password@123"
        );

        Assert.assertTrue(
            dashboard.isDashboardDisplayed(),
            "Dashboard should be visible after login"
        );
        ExtentReportManager.getTest().pass("Login successful - Dashboard displayed");
    }

    // ── TC02: Invalid Password ─────────────────────────────
    @Test(priority = 2, description = "Verify error message on invalid password")
    public void testInvalidPassword() {
        ExtentReportManager.createTest("TC02 - Invalid Password");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login("testuser@example.com", "WrongPassword");

        Assert.assertTrue(
            loginPage.isErrorDisplayed(),
            "Error message should be displayed"
        );
        Assert.assertEquals(
            loginPage.getErrorMessage(),
            "Invalid email or password",
            "Error message text mismatch"
        );
        ExtentReportManager.getTest().pass("Error message displayed correctly");
    }

    // ── TC03: Empty Email ──────────────────────────────────
    @Test(priority = 3, description = "Verify error on empty email field")
    public void testEmptyEmail() {
        ExtentReportManager.createTest("TC03 - Empty Email");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login("", "Password@123");

        Assert.assertTrue(
            loginPage.isErrorDisplayed(),
            "Validation error should show for empty email"
        );
        ExtentReportManager.getTest().pass("Validation error shown for empty email");
    }

    // ── TC04: Invalid Email Format ─────────────────────────
    @Test(priority = 4, description = "Verify error on invalid email format")
    public void testInvalidEmailFormat() {
        ExtentReportManager.createTest("TC04 - Invalid Email Format");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login("notanemail", "Password@123");

        Assert.assertTrue(
            loginPage.isErrorDisplayed(),
            "Validation error should show for invalid email format"
        );
        ExtentReportManager.getTest().pass("Validation error shown for invalid email");
    }

    // ── TC05: Logout Flow ──────────────────────────────────
    @Test(priority = 5, description = "Verify user can logout successfully")
    public void testLogout() {
        ExtentReportManager.createTest("TC05 - Logout Flow");

        LoginPage loginPage = new LoginPage(getDriver());
        DashboardPage dashboard = loginPage.login(
            "testuser@example.com",
            "Password@123"
        );

        Assert.assertTrue(dashboard.isDashboardDisplayed());
        LoginPage loginPageAfterLogout = dashboard.logout();

        Assert.assertFalse(
            dashboard.isDashboardDisplayed(),
            "Dashboard should not be visible after logout"
        );
        ExtentReportManager.getTest().pass("Logout successful");
    }
}
