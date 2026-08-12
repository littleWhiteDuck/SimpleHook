package me.simpleHook.feature.extension.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.preference.PreferenceCategory
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.core.base.BasePreferenceFragment
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.addPreferences
import me.simpleHook.core.ui.custom.MaterialSwitchPreference
import me.simpleHook.core.ui.custom.exitDialog
import me.simpleHook.data.ExAlgorithmConfig
import me.simpleHook.feature.extension.viewmodel.ExViewModel

class AlgorithmSettingsFragment : BasePreferenceFragment() {
    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        requireActivity().findNavController(R.id.nav_host_fragment)
    }
    private lateinit var algorithmConfig: ExAlgorithmConfig

    override fun init() {
        setDividerHeight(0)
        initMenu()
    }

    override fun canBack(): Boolean {
        return algorithmConfig == exViewModel.extensionConfig.value!!.algorithmConfig
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(exit = true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        algorithmConfig = exViewModel.extensionConfig.value!!.algorithmConfig.deepCopy()
        val type = arguments?.getString(ARG_TYPE) ?: TYPE_DIGEST
        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = when (type) {
                TYPE_HMAC -> getString(R.string.extension_algorithm_title_hmac_options)
                TYPE_CIPHER -> getString(R.string.extension_algorithm_title_cipher_options)
                else -> getString(R.string.extension_algorithm_title_digest_options)
            }
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        when (type) {
            TYPE_HMAC -> preferenceCategory.addPreferences(*hmacSwitches())
            TYPE_CIPHER -> preferenceCategory.addPreferences(*cipherSwitches())
            else -> preferenceCategory.addPreferences(*digestSwitches())
        }
        setPreferenceScreen(preferenceScreen)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.isVerticalScrollBarEnabled = false
        recyclerView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(recyclerView, windowInsets)
            recyclerView.setPadding(0, 0, 0, navigationInsets.bottom + 16.dp)
            windowInsets
        }
        return recyclerView
    }

    private fun digestSwitches(): Array<MaterialSwitchPreference> {
        val options = algorithmConfig.messageDigestOptions
        return arrayOf(
            switchPreference("MD5", options.md5) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(md5 = checked)
                )
            },
            switchPreference("SHA-1", options.sha1) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sha1 = checked)
                )
            },
            switchPreference("SHA-224", options.sha224) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sha224 = checked)
                )
            },
            switchPreference("SHA-256", options.sha256) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sha256 = checked)
                )
            },
            switchPreference("SHA-384", options.sha384) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sha384 = checked)
                )
            },
            switchPreference("SHA-512", options.sha512) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sha512 = checked)
                )
            },
            switchPreference("SHA-3/SHAKE", options.sha3) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sha3 = checked)
                )
            },
            switchPreference("SM3", options.sm3) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(sm3 = checked)
                )
            },
            switchPreference(getString(R.string.extension_algorithm_other_digest), options.other) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    messageDigestOptions = algorithmConfig.messageDigestOptions.copy(other = checked)
                )
            }
        )
    }

    private fun hmacSwitches(): Array<MaterialSwitchPreference> {
        val options = algorithmConfig.hmacOptions
        return arrayOf(
            switchPreference("HmacMD5", options.hmacMd5) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacMd5 = checked)
                )
            },
            switchPreference("HmacSHA1", options.hmacSha1) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSha1 = checked)
                )
            },
            switchPreference("HmacSHA224", options.hmacSha224) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSha224 = checked)
                )
            },
            switchPreference("HmacSHA256", options.hmacSha256) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSha256 = checked)
                )
            },
            switchPreference("HmacSHA384", options.hmacSha384) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSha384 = checked)
                )
            },
            switchPreference("HmacSHA512", options.hmacSha512) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSha512 = checked)
                )
            },
            switchPreference("HmacSHA3/HmacSHAKE", options.hmacSha3) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSha3 = checked)
                )
            },
            switchPreference("HmacSM3", options.hmacSm3) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(hmacSm3 = checked)
                )
            },
            switchPreference(getString(R.string.extension_algorithm_other_hmac), options.other) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    hmacOptions = algorithmConfig.hmacOptions.copy(other = checked)
                )
            }
        )
    }

    private fun cipherSwitches(): Array<MaterialSwitchPreference> {
        val options = algorithmConfig.cipherOptions
        return arrayOf(
            switchPreference("AES", options.aes) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(aes = checked)
                )
            },
            switchPreference("DES", options.des) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(des = checked)
                )
            },
            switchPreference("3DES/DESede", options.tripleDes) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(tripleDes = checked)
                )
            },
            switchPreference("RSA", options.rsa) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(rsa = checked)
                )
            },
            switchPreference("SM2", options.sm2) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(sm2 = checked)
                )
            },
            switchPreference("SM4", options.sm4) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(sm4 = checked)
                )
            },
            switchPreference("ChaCha20", options.chacha20) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(chacha20 = checked)
                )
            },
            switchPreference("RC4/ARC4", options.rc4) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(rc4 = checked)
                )
            },
            switchPreference("PBE", options.pbe) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(pbe = checked)
                )
            },
            switchPreference(getString(R.string.extension_algorithm_other_cipher), options.other) { checked ->
                algorithmConfig = algorithmConfig.copy(
                    cipherOptions = algorithmConfig.cipherOptions.copy(other = checked)
                )
            }
        )
    }

    private fun switchPreference(
        titleText: String,
        checked: Boolean,
        onChange: (Boolean) -> Unit
    ): MaterialSwitchPreference {
        return MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = titleText
            isIconSpaceReserved = false
            isChecked = checked
            setOnPreferenceChangeListener { _, newValue ->
                onChange(newValue as Boolean)
                true
            }
        }
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_file_monitor, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_save -> saveConfig(true)
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun saveConfig(exit: Boolean) {
        exViewModel.updateAlgorithmConfig(algorithmConfig)
        if (exit) navController.navigateUp()
    }

    private fun ExAlgorithmConfig.deepCopy(): ExAlgorithmConfig {
        return copy(
            messageDigestOptions = messageDigestOptions.copy(),
            hmacOptions = hmacOptions.copy(),
            cipherOptions = cipherOptions.copy()
        )
    }

    companion object {
        const val ARG_TYPE = "algorithm_type"
        const val TYPE_DIGEST = "digest"
        const val TYPE_HMAC = "hmac"
        const val TYPE_CIPHER = "cipher"
    }
}
