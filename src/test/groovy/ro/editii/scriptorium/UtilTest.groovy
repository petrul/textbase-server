package ro.editii.scriptorium

import com.ibm.icu.text.Transliterator
import org.junit.jupiter.api.Test

import static ro.editii.scriptorium.Util.urlFriendify

class UtilTest {

    @Test
    void urlFriendify() {
        assert urlFriendify('a+b') == 'a_b'
        assert urlFriendify("1ª parte") == '1a_parte'
        assert urlFriendify('4º parte') == '4o_parte'
        assert urlFriendify('De Ascalapho V° Acherontis filio') == 'de_ascalapho_vo_acherontis_filio'
        assert urlFriendify('del“beg')  == 'delbeg'
        assert urlFriendify('ăàαªâ') == 'aaaaa'
        assert urlFriendify('øöôòº°') == 'oooooo'
        assert urlFriendify('αβγ') == 'abg'
        assert urlFriendify('Ἀνάγκη') == 'ananke'
        assert urlFriendify('無聲戲') == 'wu_sheng_xi'
        assert urlFriendify('जनश्रुतियाँ प्रचलित हैं, पर') == 'janasrutiyam_pracalita_haim_para'

        final devanagariExcerpt = '''
        हालाँकि सूर के जीवन के बारे में कई जनश्रुतियाँ प्रचलित हैं, 
        पर इन में कितनी सच्चाई है यह कहना कठिन है। कहा जाता है 
        उनका जन्म सन् १४७८ में दिल्ली के पास एक ग़रीब ब्राह्मीण 
        परिवार में हुआ। जनश्रुति के अनुसार सूरदास जन्म 
        से ही अंधे थे। आजकल थी अंधे आदमी अक्सर 
        'सूरदास' कहलाते हैं। कई लोगों ने उन्हें गुरु के 
        रूप में अपनाया और उनकी पूजा करना शुरु कर दिया ।
        '''
        final deva2latin = urlFriendify(devanagariExcerpt)
        assert  deva2latin =~ /^[a-zA-Z0-9_]+$/

        assert urlFriendify('«Грядой клубится белою…»') == 'gryadoi_klubitsya_beloyu'
        assert urlFriendify('Колышется море; волна за волной…') == 'kolyshetsya_more_volna_za_volnoi'
    }

    @Test
    void testExpectations() {

        final expected = [
            '_1_'           : '1',
            ' -_- 2 - - '   : '2',
            'L’évasion': 'l_evasion',
            'L’Homme à l’oreille cassée': 'l_homme_a_l_oreille_cassee'
        ]

        expected.forEach {k, v -> assert urlFriendify(k) == v }

    }

    @Test
    void transliterators() {
        def ids = Transliterator.availableIDs.toList()
        assert ! ids.empty
    }

    @Test
    void extension() {
        assert 'txt' == Util.getExtension('/creanga/povesti.txt')
        assert null == Util.getExtension('/creanga/povesti')

        assert '/creanga/povesti' == Util.basename('/creanga/povesti.txt')
        assert '/creanga/povesti' == Util.basename('/creanga/povesti')
    }

    @Test
    void noUnderscoresAtTheBeginning() {
        assert '1' == Util.urlFriendify('_1')
        assert '1' == Util.urlFriendify('__1')

    }

    @Test
    void symbols() {
        assert 'percent_1' == Util.urlFriendify('_-%1')
        assert 'hash_1' == Util.urlFriendify("#1")
    }

}