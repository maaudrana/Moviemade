package org.michaelbel.moviemade.presentation.features.login

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.transaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.readystatesoftware.chuck.internal.ui.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_account.*
import kotlinx.android.synthetic.main.view_login.*
import org.michaelbel.core.customtabs.Browser
import org.michaelbel.moviemade.R
import org.michaelbel.moviemade.core.TmdbConfig.GRAVATAR_URL
import org.michaelbel.moviemade.core.TmdbConfig.REDIRECT_URL
import org.michaelbel.moviemade.core.TmdbConfig.TMDB_AUTH_URL
import org.michaelbel.moviemade.core.TmdbConfig.TMDB_LOGO
import org.michaelbel.moviemade.core.TmdbConfig.TMDB_PRIVACY_POLICY
import org.michaelbel.moviemade.core.TmdbConfig.TMDB_REGISTER
import org.michaelbel.moviemade.core.TmdbConfig.TMDB_RESET_PASSWORD
import org.michaelbel.moviemade.core.TmdbConfig.TMDB_TERMS_OF_USE
import org.michaelbel.moviemade.core.ViewUtil
import org.michaelbel.moviemade.core.errors.Error
import org.michaelbel.moviemade.core.local.SharedPrefs.KEY_ACCOUNT_ID
import org.michaelbel.moviemade.core.local.SharedPrefs.KEY_DATE_AUTHORISED
import org.michaelbel.moviemade.core.local.SharedPrefs.KEY_SESSION_ID
import org.michaelbel.moviemade.core.local.SharedPrefs.KEY_TOKEN
import org.michaelbel.moviemade.presentation.App
import org.michaelbel.moviemade.presentation.common.base.BaseFragment
import org.michaelbel.moviemade.presentation.features.user.UserFragment
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

class LoginFragment: BaseFragment() {

    companion object {
        internal fun newInstance() = LoginFragment()
    }

    private var accountId: Int = 0
    private var sessionId: String = ""

    private lateinit var viewModel: LoginModel

    @Inject
    lateinit var factory: LoginFactory

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App[requireActivity().application].createFragmentComponent().inject(this)
        sessionId = preferences.getString(KEY_SESSION_ID, "") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(requireActivity(), factory).get(LoginModel::class.java)
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLogin()

        signinBtn.setOnClickListener {
            val name = username.text.toString().trim()
            val pass = password.text.toString().trim()

            if (name.isEmpty() || pass.isEmpty()) {
                Toast.makeText(activity, R.string.msg_enter_data, Toast.LENGTH_SHORT).show()
            } else {
                viewModel.createRequestToken(name, pass)
            }
        }

        signupBtn.setOnClickListener { Browser.openUrl(requireContext(), TMDB_REGISTER) }
        resetBtn.setOnClickListener { Browser.openUrl(requireContext(), TMDB_RESET_PASSWORD) }
        loginBtn.setOnClickListener { viewModel.createRequestToken() }
        termsBtn.setOnClickListener { Browser.openUrl(requireContext(), TMDB_TERMS_OF_USE) }
        privacyBtn.setOnClickListener { Browser.openUrl(requireContext(), TMDB_PRIVACY_POLICY) }

        viewModel.sessionCreated.observe(viewLifecycleOwner, Observer {
            username.text?.clear()
            password.text?.clear()

            preferences.edit().putString(KEY_SESSION_ID, sessionId).apply()
            showAccount()
        })
        viewModel.error.observe(viewLifecycleOwner, Observer {
            when (it) {
                Error.ERROR_UNAUTHORIZED -> {
                    preferences.edit().putString(KEY_SESSION_ID, "").apply()
                    loginLayout.visibility = VISIBLE
                    privacyBtn.visibility = GONE
                }
                Error.ERROR_CONNECTION_NO_TOKEN ->
                    Toast.makeText(requireContext(), R.string.error_empty_token, Toast.LENGTH_SHORT).show()
                Error.ERR_NO_CONNECTION ->
                    Toast.makeText(requireContext(), R.string.error_no_connection, Toast.LENGTH_SHORT).show()
                Error.ERROR_AUTH_WITH_LOGIN ->
                    Toast.makeText(requireContext(), R.string.error_invalid_data, Toast.LENGTH_SHORT).show()
                Error.ERROR_NOT_FOUND ->
                    Toast.makeText(requireContext(), R.string.error_not_found, Toast.LENGTH_SHORT).show()
            }
        })
        viewModel.throwable.observe(viewLifecycleOwner, Observer {
            val code = (it as HttpException).code()
            if (code == 401) {
                preferences.edit().putString(KEY_SESSION_ID, "").apply()
                loginLayout.visibility = VISIBLE
                privacyBtn.visibility = GONE
            } else if (code == 404) {
                Toast.makeText(requireContext(), R.string.error_not_found, Toast.LENGTH_SHORT).show()
            }
        })
        viewModel.account.observe(viewLifecycleOwner, Observer {
            accountId = it.id
            preferences.edit().putInt(KEY_ACCOUNT_ID, accountId).apply()
            loginText.text = if (it.username.isEmpty()) getString(R.string.none) else it.username
            nameText.text = if (it.name.isEmpty()) getString(R.string.none) else it.name
            Glide.with(requireContext())
                    .load(String.format(Locale.US, GRAVATAR_URL, it.avatar.gravatar.hash))
                    .thumbnail(0.1F).into(avatar)
        })
        viewModel.browserAuth.observe(viewLifecycleOwner, Observer {
            Browser.openUrl(requireContext(), String.format(TMDB_AUTH_URL, it, REDIRECT_URL))
        })
        viewModel.token.observe(viewLifecycleOwner, Observer {
            preferences.edit {
                putString(KEY_TOKEN, it.requestToken)
                putString(KEY_DATE_AUTHORISED, it.date)
            }
        })
    }

    override fun onNewIntent(action: String?, data: String?) {
        if (Intent.ACTION_VIEW == action && data != null) {
            val token = preferences.getString(KEY_TOKEN, "") ?: ""
            viewModel.createSessionId(token)
        }
    }

    override fun onScrollToTop() {
        super.onScrollToTop()
    }

    private fun showLogin() {
        preferences.edit().putString(KEY_SESSION_ID, "").apply()
        accountLayout.visibility = GONE
        loginLayout.visibility = VISIBLE

        Glide.with(requireContext()).load(TMDB_LOGO).thumbnail(0.1F).into(logoImage)
        ViewUtil.clearCursorDrawable(username)
        ViewUtil.clearCursorDrawable(password)
        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_ACTION_DONE) {
                signinBtn.performClick()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun showAccount() {
        hideKeyboard(password)
        requireFragmentManager().transaction {
            replace((requireActivity() as MainActivity).container.id, UserFragment.newInstance())
        }
    }

    private fun hideKeyboard(view: View?) {
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive.not()) {
            return
        }
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}