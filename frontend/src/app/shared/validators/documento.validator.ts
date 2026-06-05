import { AbstractControl, ValidationErrors } from '@angular/forms';

export function documentoValidator(control: AbstractControl): ValidationErrors | null {
  const raw = (control.value || '').replace(/\D/g, '');
  if (!raw) return null; // required cuida do vazio

  if (raw.length === 11) {
    return validarCpf(raw) ? null : { documento: 'CPF inválido' };
  }
  if (raw.length === 14) {
    return validarCnpj(raw) ? null : { documento: 'CNPJ inválido' };
  }

  return { documento: 'Documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos' };
}

function validarCpf(cpf: string): boolean {
  if (/^(\d)\1{10}$/.test(cpf)) return false;

  let soma = 0;
  for (let i = 0; i < 9; i++) soma += parseInt(cpf[i]) * (10 - i);
  let resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  if (resto !== parseInt(cpf[9])) return false;

  soma = 0;
  for (let i = 0; i < 10; i++) soma += parseInt(cpf[i]) * (11 - i);
  resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  return resto === parseInt(cpf[10]);
}

function validarCnpj(cnpj: string): boolean {
  if (/^(\d)\1{13}$/.test(cnpj)) return false;

  const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesos2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  let soma = 0;
  for (let i = 0; i < 12; i++) soma += parseInt(cnpj[i]) * pesos1[i];
  let resto = soma % 11;
  const dig1 = resto < 2 ? 0 : 11 - resto;
  if (dig1 !== parseInt(cnpj[12])) return false;

  soma = 0;
  for (let i = 0; i < 13; i++) soma += parseInt(cnpj[i]) * pesos2[i];
  resto = soma % 11;
  const dig2 = resto < 2 ? 0 : 11 - resto;
  return dig2 === parseInt(cnpj[13]);
}
