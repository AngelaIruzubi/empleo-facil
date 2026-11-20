"""Tests de checks.py. Uso: python -m unittest discover evals"""

import unittest

from checks import lost_numbers, pictogram_check, wrong_weekdays


class LostNumbersTest(unittest.TestCase):

    def test_importes_escritos_de_otra_forma_cuentan_como_conservados(self):
        self.assertEqual(lost_numbers("Salario de 1.300 € y 9,80 €/h", "Ganas 1300 euros. Son 9.80 euros por hora."), [])

    def test_un_numero_que_falta_se_marca(self):
        self.assertEqual(lost_numbers("Contrato de 6 meses. 20 h semanales.", "Trabajas 20 horas cada semana."), ["6"])

    def test_una_hora_en_punto_puede_ir_como_la_tarde(self):
        self.assertEqual(lost_numbers("De 17:00 a 21:00", "Trabajas de 5 de la tarde a 9 de la noche."), [])

    def test_una_hora_con_minutos_tiene_que_estar(self):
        self.assertEqual(lost_numbers("Entrada a las 7:30", "Entras a las 7 de la mañana."), ["7:30"])

    def test_los_numeros_escritos_con_letras_cuentan(self):
        self.assertEqual(lost_numbers("Experiencia de 1 año", "Necesitas un año de experiencia."), [])


class WrongWeekdaysTest(unittest.TestCase):

    def test_un_dia_que_no_corresponde_se_marca(self):
        # El caso real de la fase 3: el 1 de diciembre de 2026 es martes
        self.assertEqual(wrong_weekdays("Empiezas el lunes 1 de diciembre de 2026.", 2026),
                         ["lunes 1 de diciembre de 2026 es martes"])

    def test_un_dia_correcto_no_se_marca(self):
        self.assertEqual(wrong_weekdays("Empiezas el martes 1 de diciembre de 2026.", 2026), [])

    def test_sin_año_usa_el_de_la_oferta(self):
        self.assertEqual(wrong_weekdays("Confirma antes del jueves 14 de enero.", 2027), [])


class PictogramCheckTest(unittest.TestCase):
    OFFER = {"oferta": True, "profesiones_validas": ["limpiador", "limpiadora"]}
    NOT_OFFER = {"oferta": False, "profesiones_validas": []}

    def test_profesion_valida(self):
        self.assertEqual(pictogram_check({"pictogram": {"keyword": "Limpiador"}}, self.OFFER), "bien")

    def test_profesion_equivocada(self):
        self.assertEqual(pictogram_check({"pictogram": {"keyword": "auxiliar"}}, self.OFFER), "pictograma equivocado: auxiliar")

    def test_una_oferta_sin_pictograma(self):
        self.assertEqual(pictogram_check({"pictogram": None}, self.OFFER), "sin pictograma")

    def test_lo_que_no_es_oferta_no_lleva_pictograma(self):
        self.assertEqual(pictogram_check({"pictogram": {"keyword": "almacenista"}}, self.NOT_OFFER),
                         "pictograma de más: almacenista")


if __name__ == "__main__":
    unittest.main()
